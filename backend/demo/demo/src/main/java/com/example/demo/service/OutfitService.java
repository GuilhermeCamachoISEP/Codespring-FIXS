package com.example.demo.service;

import com.example.demo.domain.UserPreferences;
import com.example.demo.domain.WardrobeItem;
import com.example.demo.dto.OutfitSuggestion;
import com.example.demo.dto.WeatherData;
import com.example.demo.domain.OutfitHistory;
import com.example.demo.repository.UserPreferencesRepository;
import com.example.demo.repository.WardrobeItemRepository;
import com.example.demo.repository.OutfitReservationRepository;
import com.example.demo.domain.OutfitReservation;
import java.time.LocalDate;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class OutfitService {

    private final WardrobeItemRepository wardrobeItemRepository;
    private final UserPreferencesRepository preferencesRepository;
    private final OutfitReservationRepository reservationRepository;
    private final ClaudeService claudeService;
    private final ObjectMapper objectMapper;
    private final OutfitHistoryService historyService;

    public OutfitService(WardrobeItemRepository wardrobeItemRepository,
                         UserPreferencesRepository preferencesRepository,
                         OutfitReservationRepository reservationRepository,
                         ClaudeService claudeService,
                         ObjectMapper objectMapper,
                         OutfitHistoryService historyService) {
        this.wardrobeItemRepository = wardrobeItemRepository;
        this.preferencesRepository = preferencesRepository;
        this.reservationRepository = reservationRepository;
        this.claudeService = claudeService;
        this.objectMapper = objectMapper;
        this.historyService = historyService;
    }

    public List<OutfitSuggestion> generateOutfits(Long userId, WeatherData weather) {
        System.out.println("[DEBUG-OUTFIT] generateOutfits called for userId=" + userId);
        List<WardrobeItem> items = wardrobeItemRepository.findByUserIdOrderByCreatedAtDesc(userId);
        System.out.println("[DEBUG-OUTFIT] Total items for user: " + items.size());
        for(WardrobeItem item : items) {
            System.out.println("[DEBUG-OUTFIT] Item ID=" + item.getId() + " | Category='" + item.getCategory() + "' | SubCat='" + item.getSubcategory() + "'");
        }
        if (items.isEmpty()) return List.of();

        List<Long> reservedItemIds = reservationRepository.findByUserIdAndEventDate(userId, LocalDate.now())
                .stream().map(r -> r.getWardrobeItem().getId()).toList();
        
        List<WardrobeItem> availableItems = items.stream()
                .filter(item -> !reservedItemIds.contains(item.getId()))
                .toList();
        System.out.println("[DEBUG-OUTFIT] Available items after removing " + reservedItemIds.size() + " reserved: " + availableItems.size());
        
        if (availableItems.isEmpty()) {
            System.out.println("[DEBUG-OUTFIT] No available items, returning empty.");
            return List.of();
        }

        String styleWeights = preferencesRepository.findByUserId(userId)
                .map(UserPreferences::getStyleWeights)
                .orElse("{}");

        List<OutfitHistory> recentHistory = historyService.getLast7Outfits(userId);
        List<OutfitHistory> likedHistory = historyService.getLikedOutfits(userId);

        String prompt = buildPrompt(availableItems, styleWeights, weather, null, recentHistory, likedHistory);
        System.out.println("[DEBUG-OUTFIT] Calling Claude/Groq API...");
        String json = claudeService.generateOutfitsRaw(prompt);
        System.out.println("[DEBUG-OUTFIT] AI Raw JSON: " + json);
        List<OutfitSuggestion> aiOutfits = parseOutfits(json, availableItems);
        System.out.println("[DEBUG-OUTFIT] Parsed AI outfits size: " + aiOutfits.size());
        if (!aiOutfits.isEmpty()) {
            return aiOutfits;
        }
        System.out.println("[DEBUG-OUTFIT] Calling fallback generator...");
        List<OutfitSuggestion> fallback = generateFallbackOutfits(availableItems, styleWeights);
        System.out.println("[DEBUG-OUTFIT] Fallback outfits size: " + fallback.size());
        return fallback;
    }

    public List<OutfitSuggestion> generateOutfitsForEvent(Long userId, String eventName, WeatherData weather, LocalDate eventDate) {
        List<WardrobeItem> items = wardrobeItemRepository.findByUserIdOrderByCreatedAtDesc(userId);
        if (items.isEmpty()) return List.of();
        
        List<Long> reservedItemIds = reservationRepository.findByUserIdAndEventDate(userId, eventDate)
                .stream()
                .filter(r -> !r.getEventName().equals(eventName))
                .map(r -> r.getWardrobeItem().getId())
                .toList();

        List<WardrobeItem> availableItems = items.stream()
                .filter(item -> !reservedItemIds.contains(item.getId()))
                .toList();

        if (availableItems.isEmpty()) return List.of();

        String styleWeights = preferencesRepository.findByUserId(userId)
                .map(UserPreferences::getStyleWeights)
                .orElse("{}");

        List<OutfitHistory> recentHistory = historyService.getLast7Outfits(userId);
        List<OutfitHistory> likedHistory = historyService.getLikedOutfits(userId);

        String prompt = buildPrompt(availableItems, styleWeights, weather, eventName, recentHistory, likedHistory);
        String json = claudeService.generateOutfitsRaw(prompt);
        List<OutfitSuggestion> aiOutfits = parseOutfits(json, availableItems);
        if (!aiOutfits.isEmpty()) {
            return aiOutfits;
        }
        return generateFallbackOutfits(availableItems, styleWeights);
    }

    private String buildPrompt(List<WardrobeItem> items, String styleWeights, WeatherData weather, String eventName, List<OutfitHistory> recentHistory, List<OutfitHistory> likedHistory) {
        try {
            var itemNodes = objectMapper.createArrayNode();
            for (WardrobeItem item : items) {
                var node = objectMapper.createObjectNode()
                        .put("id", item.getId())
                        .put("category", item.getCategory())
                        .put("subcategory", item.getSubcategory())
                        .put("color", item.getColor())
                        .put("fit", item.getFit())
                        .put("material", item.getMaterial());
                String tagsJson = item.getStyleTags() != null ? item.getStyleTags() : "[]";
                node.set("styleTags", objectMapper.readTree(tagsJson));
                itemNodes.add(node);
            }

            String weatherContext = buildWeatherContext(weather);
            String recentOutfitsStr = "[]";
            if (!recentHistory.isEmpty()) {
                var recentArray = objectMapper.createArrayNode();
                for (OutfitHistory h : recentHistory) {
                    var historyIds = objectMapper.createArrayNode();
                    for (JsonNode itemNode : objectMapper.readTree(h.getOutfitItems())) {
                        historyIds.add(itemNode.path("id").asLong());
                    }
                    recentArray.add(historyIds);
                }
                recentOutfitsStr = objectMapper.writeValueAsString(recentArray);
            }

            String likedOutfitsStr = "[]";
            if (!likedHistory.isEmpty()) {
                var likedArray = objectMapper.createArrayNode();
                for (OutfitHistory h : likedHistory) {
                    var historyIds = objectMapper.createArrayNode();
                    for (JsonNode itemNode : objectMapper.readTree(h.getOutfitItems())) {
                        historyIds.add(itemNode.path("id").asLong());
                    }
                    likedArray.add(historyIds);
                }
                likedOutfitsStr = objectMapper.writeValueAsString(likedArray);
            }

            String eventContext = eventName != null ? "\nCRIAR OUTFITS ESPECÍFICAMENTE PARA O EVENTO: " + eventName + "\nA temática do evento é a principal prioridade na escolha das peças." : "";

            return """
                    És um stylist de moda pessoal. Com base nas preferências de estilo e no armário do utilizador, cria combinações de outfits.%s
                    %s
                    Preferências de estilo do utilizador (pesos de 0 a 1):
                    %s

                    Peças disponíveis no armário:
                    %s
                    
                    RECENT OUTFITS WORN (try to avoid exact repetition if possible, but you MUST generate an outfit even if you have to repeat):
                    %s
                    
                    LIKED OUTFITS (user loves these — use as style reference):
                    %s
                    
                    If wardrobe is too limited to avoid repetition, just repeat a recent outfit but mention in the description: 'You've been wearing similar combinations — time to add more variety!'.

                    Cria exatamente 1 outfit completo. Regras:
                    - O outfit deve ter pelo menos uma parte de cima (tops ou jackets) e uma de baixo (bottoms) ou sapatos (shoes)
                    - As peças devem combinar em cor e estilo
                    - Respeita as preferências de estilo do utilizador
                    - Usa APENAS os IDs das peças da lista acima
                    - O nome e descrição devem ser em português de Portugal
                    - O campo "weatherNote" deve ser uma nota curta (máx. 6 palavras) sobre como o outfit se adequa ao clima atual (ex: "Perfeito para este frio", "Ideal para dia de chuva")

                    Responde APENAS com um JSON array com 1 elemento, sem markdown:
                    [
                      {
                        "name": "Nome do outfit",
                        "description": "Descrição curta do look",
                        "itemIds": [1, 4, 7],
                        "weatherNote": "Perfeito para este frio"
                      }
                    ]
                    """.formatted(eventContext, weatherContext, styleWeights, objectMapper.writeValueAsString(itemNodes), recentOutfitsStr, likedOutfitsStr);
        } catch (Exception e) {
            throw new RuntimeException("Failed to build outfit prompt", e);
        }
    }

    private String buildWeatherContext(WeatherData weather) {
        if (weather == null) return "";

        boolean rainy = weather.getWeatherCode() >= 51 && weather.getWeatherCode() <= 82;
        String rainNote = rainy ? "\n    - Está a chover: prefere casacos ou jaquetas impermeáveis." : "";

        return """

                    CONTEXTO METEOROLÓGICO ATUAL (%s):
                    - Temperatura: %.1f°C (%s)
                    - Condição: %s
                    - Vento: %.0f km/h
                    - Orientação por temperatura: very-cold(<5°C)=casacos pesados e camadas; cold(5-12°C)=casaco quente; cool(12-18°C)=jaqueta leve; mild(18-24°C)=t-shirt com camada opcional; warm(24-30°C)=roupa leve; hot(>30°C)=roupa muito leve e respirável%s
                    IMPORTANTE: adapta TODOS os outfits ao clima atual.
                    """.formatted(
                weather.getCity(),
                weather.getTemperature(),
                weather.getTempCategory(),
                weather.getDescription(),
                weather.getWindSpeed(),
                rainNote
        );
    }

    public List<OutfitSuggestion> parseOutfitSuggestions(String json, List<WardrobeItem> items) {
        return parseOutfits(json, items);
    }

    private List<OutfitSuggestion> parseOutfits(String json, List<WardrobeItem> items) {
        try {
            Map<Long, WardrobeItem> itemMap = items.stream()
                    .collect(Collectors.toMap(WardrobeItem::getId, Function.identity()));

            JsonNode root = objectMapper.readTree(json);
            if (!root.isArray()) return List.of();

            List<OutfitSuggestion> result = new ArrayList<>();
            for (JsonNode outfit : root) {
                String name = outfit.path("name").asText("Outfit");
                String description = outfit.path("description").asText("");
                List<WardrobeItem> outfitItems = new ArrayList<>();

                for (JsonNode idNode : outfit.path("itemIds")) {
                    WardrobeItem item = itemMap.get(idNode.asLong());
                    if (item != null) outfitItems.add(item);
                }

                if (!outfitItems.isEmpty()) {
                    String weatherNote = outfit.path("weatherNote").asText(null);
                    if (weatherNote != null && (weatherNote.isBlank() || "null".equals(weatherNote))) {
                        weatherNote = null;
                    }
                    result.add(new OutfitSuggestion(name, description, outfitItems, weatherNote));
                }
            }
            return result;
        } catch (Exception e) {
            System.err.println("Failed to parse outfit suggestions: " + e.getMessage());
            return List.of();
        }
    }

    private List<OutfitSuggestion> generateFallbackOutfits(List<WardrobeItem> items, String styleWeights) {
        List<String> preferredStyles = parsePreferredStyles(styleWeights);
        List<WardrobeItem> sorted = items.stream()
                .sorted(Comparator.comparingDouble((WardrobeItem item) -> scoreItem(item, preferredStyles)).reversed())
                .toList();

        List<WardrobeItem> tops = byCategory(sorted, "tops");
        List<WardrobeItem> jackets = byCategory(sorted, "jackets");
        List<WardrobeItem> bottoms = byCategory(sorted, "bottoms");
        List<WardrobeItem> shoes = byCategory(sorted, "shoes");
        List<WardrobeItem> accessories = byCategory(sorted, "accessories");

        System.out.println("[DEBUG-FALLBACK] Categorized -> Tops:" + tops.size() + " Jackets:" + jackets.size() + " Bottoms:" + bottoms.size() + " Shoes:" + shoes.size());

        if (tops.isEmpty() && jackets.isEmpty()) {
            System.out.println("[DEBUG-FALLBACK] Missing upper parts. Returning empty.");
            return List.of();
        }
        if (bottoms.isEmpty() && shoes.isEmpty()) {
            System.out.println("[DEBUG-FALLBACK] Missing lower parts. Returning empty.");
            return List.of();
        }

        List<OutfitSuggestion> outfits = new ArrayList<>();
        int target = 1;
        for (int i = 0; i < target; i++) {
            List<WardrobeItem> outfitItems = new ArrayList<>();
            WardrobeItem top = pick(tops.isEmpty() ? jackets : tops, i);
            WardrobeItem bottom = pick(bottoms, i);
            WardrobeItem shoe = pick(shoes, i);
            WardrobeItem jacket = pick(jackets, i);
            WardrobeItem accessory = pick(accessories, i);

            addIfPresent(outfitItems, top);
            addIfPresent(outfitItems, bottom);
            addIfPresent(outfitItems, shoe);
            if (jacket != null && !outfitItems.contains(jacket) && i % 2 == 0) addIfPresent(outfitItems, jacket);
            if (accessory != null && i % 2 == 1) addIfPresent(outfitItems, accessory);

            if (outfitItems.size() >= 2) {
                String styleName = preferredStyles.isEmpty() ? "casual" : preferredStyles.get(i % preferredStyles.size());
                outfits.add(new OutfitSuggestion(
                        "Look " + (i + 1) + " " + readableStyle(styleName),
                        describeOutfit(outfitItems, styleName),
                        outfitItems
                ));
            }
        }
        return outfits.stream()
                .filter(outfit -> hasUsefulCombination(outfit.getItems()))
                .limit(1)
                .toList();
    }

    private List<WardrobeItem> byCategory(List<WardrobeItem> items, String category) {
        return items.stream()
                .filter(item -> category.equals(item.getCategory()))
                .toList();
    }

    private WardrobeItem pick(List<WardrobeItem> items, int index) {
        if (items.isEmpty()) return null;
        return items.get(index % items.size());
    }

    private void addIfPresent(List<WardrobeItem> items, WardrobeItem item) {
        if (item != null && !items.contains(item)) {
            items.add(item);
        }
    }

    private boolean hasUsefulCombination(List<WardrobeItem> items) {
        boolean hasUpper = items.stream().anyMatch(item -> "tops".equals(item.getCategory()) || "jackets".equals(item.getCategory()));
        boolean hasAnchor = items.stream().anyMatch(item -> "bottoms".equals(item.getCategory()) || "shoes".equals(item.getCategory()));
        return hasUpper && hasAnchor;
    }

    private double scoreItem(WardrobeItem item, List<String> preferredStyles) {
        double score = item.getFavoriteScore();
        List<String> tags = parseTags(item.getStyleTags());
        for (int i = 0; i < preferredStyles.size(); i++) {
            if (tags.contains(preferredStyles.get(i))) {
                score += 10 - i;
            }
        }
        return score;
    }

    private List<String> parsePreferredStyles(String styleWeights) {
        try {
            JsonNode root = objectMapper.readTree(styleWeights == null ? "{}" : styleWeights);
            List<String> styles = new ArrayList<>();
            root.fields().forEachRemaining(entry -> styles.add(entry.getKey()));
            return styles;
        } catch (Exception e) {
            return List.of();
        }
    }

    private List<String> parseTags(String tagsJson) {
        try {
            JsonNode root = objectMapper.readTree(tagsJson == null ? "[]" : tagsJson);
            List<String> tags = new ArrayList<>();
            root.forEach(tag -> tags.add(tag.asText()));
            return tags;
        } catch (Exception e) {
            return List.of();
        }
    }

    private String readableStyle(String style) {
        return switch (style) {
            case "oldMoney" -> "Old Money";
            case "darkAcademia" -> "Dark Academia";
            case "quietLuxury" -> "Quiet Luxury";
            case "streetwear" -> "Streetwear";
            case "gorpcore" -> "Gorpcore";
            case "minimalist" -> "Minimalista";
            case "techwear" -> "Techwear";
            case "preppy" -> "Preppy";
            case "vintage" -> "Vintage";
            case "luxury" -> "Luxury";
            case "y2k" -> "Y2K";
            default -> "Casual";
        };
    }

    private String describeOutfit(List<WardrobeItem> items, String style) {
        String pieces = items.stream()
                .map(item -> safeText(item.getColor()) + " " + safeText(item.getSubcategory()))
                .collect(Collectors.joining(", "));
        return "Combina " + pieces + " com uma leitura " + readableStyle(style) + ".";
    }

    public void reserveOutfit(Long userId, String eventName, LocalDate eventDate, List<Long> itemIds) {
        for (Long itemId : itemIds) {
            wardrobeItemRepository.findById(itemId).ifPresent(item -> {
                OutfitReservation res = new OutfitReservation(userId, item, eventDate, eventName);
                reservationRepository.save(res);
            });
        }
    }

    private String safeText(String value) {

        return value == null || value.isBlank() ? "peça" : value;
    }
}
