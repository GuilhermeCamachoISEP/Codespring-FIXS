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

        // Collect item IDs worn in the last 2 days to deprioritize them
        java.util.Set<Long> recentlyWornItemIds = new java.util.HashSet<>();
        try {
            for (OutfitHistory h : historyService.getWornSince(userId, 2)) {
                com.fasterxml.jackson.databind.JsonNode nodes = new com.fasterxml.jackson.databind.ObjectMapper().readTree(h.getOutfitItems());
                if (nodes.isArray()) {
                    for (com.fasterxml.jackson.databind.JsonNode n : nodes) {
                        long itemId = n.path("id").asLong(-1);
                        if (itemId > 0) recentlyWornItemIds.add(itemId);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[OutfitService] Could not parse recent worn items: " + e.getMessage());
        }

        // RAG: Semantic Search with Groq Embeddings
        Map<Long, Double> ragScores = applySemanticRAG(availableItems, styleWeights, likedHistory);
        if (!ragScores.isEmpty()) {
            availableItems = new ArrayList<>(availableItems);
            availableItems.sort((a, b) -> {
                // Penalise items worn in the last 2 days (push them to the back)
                double penaltyA = recentlyWornItemIds.contains(a.getId()) ? -0.5 : 0.0;
                double penaltyB = recentlyWornItemIds.contains(b.getId()) ? -0.5 : 0.0;
                return Double.compare(
                        ragScores.getOrDefault(b.getId(), 0.0) + penaltyB,
                        ragScores.getOrDefault(a.getId(), 0.0) + penaltyA
                );
            });
        }

        String prompt = buildPrompt(availableItems, ragScores, styleWeights, weather, null, recentHistory, likedHistory);
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

        Map<Long, Double> ragScores = applySemanticRAG(availableItems, styleWeights, likedHistory);
        if (!ragScores.isEmpty()) {
            availableItems = new ArrayList<>(availableItems);
            availableItems.sort((a, b) -> Double.compare(
                    ragScores.getOrDefault(b.getId(), 0.0),
                    ragScores.getOrDefault(a.getId(), 0.0)
            ));
        }

        String prompt = buildPrompt(availableItems, ragScores, styleWeights, weather, eventName, recentHistory, likedHistory);
        String json = claudeService.generateOutfitsRaw(prompt);
        List<OutfitSuggestion> aiOutfits = parseOutfits(json, availableItems);
        if (!aiOutfits.isEmpty()) {
            return aiOutfits;
        }
        return generateFallbackOutfits(availableItems, styleWeights);
    }

    private Map<Long, Double> applySemanticRAG(List<WardrobeItem> availableItems, String styleWeights, List<OutfitHistory> likedHistory) {
        try {
            if (availableItems.isEmpty()) return Map.of();

            String semanticProfile = styleWeights.toLowerCase() + " ";
            if (!likedHistory.isEmpty()) {
                for (OutfitHistory h : likedHistory) {
                    semanticProfile += h.getOutfitItems().toLowerCase() + " ";
                }
            }

            // Extract unique tokens for the user profile (simulating an embedding space)
            java.util.Set<String> profileTokens = new java.util.HashSet<>(java.util.Arrays.asList(semanticProfile.split("\\W+")));
            profileTokens.remove("");

            Map<Long, Double> ragScores = new java.util.HashMap<>();
            for (WardrobeItem item : availableItems) {
                String itemText = (item.getCategory() + " " + item.getSubcategory() + " " + item.getColor() + " " + item.getMaterial() + " " + item.getFit() + " " + item.getStyleTags()).toLowerCase();
                java.util.Set<String> itemTokens = new java.util.HashSet<>(java.util.Arrays.asList(itemText.split("\\W+")));
                itemTokens.remove("");
                
                // Calculate Jaccard Similarity (Intersection over Union) as a local fallback for embeddings
                java.util.Set<String> intersection = new java.util.HashSet<>(profileTokens);
                intersection.retainAll(itemTokens);
                
                java.util.Set<String> union = new java.util.HashSet<>(profileTokens);
                union.addAll(itemTokens);
                
                double score = union.isEmpty() ? 0.0 : (double) intersection.size() / union.size();
                ragScores.put(item.getId(), score);
            }
            
            System.out.println("[DEBUG-RAG] Successfully calculated Local Semantic Search (Jaccard RAG) for " + availableItems.size() + " items.");
            return ragScores;
        } catch (Exception e) {
            System.err.println("[DEBUG-RAG] Local Semantic search failed: " + e.getMessage());
            return Map.of();
        }
    }

    // ─── Event formality mapping ─────────────────────────────────────────────

    private static String resolveEventDressCode(String eventName) {
        if (eventName == null) return null;
        String lc = eventName.toLowerCase();

        if (lc.matches(".*\\b(casamento|wedding|cerimónia|cerimonia|batizado|baptism|comunhão|comunhao|gala|black.tie)\\b.*"))
            return "BLACK-TIE / CERIMÓNIA FORMAL — fato completo ou vestido formal; sem streetwear, casual ou desportivo";

        if (lc.matches(".*\\b(jantar|dinner|restaurante|restaurant|aniversário|aniversario|birthday|festa elegante|gala casual)\\b.*"))
            return "SMART-CASUAL ELEGANTE — calças bem cortadas/saia, camisa ou blusa, sapatos fechados; sem ténis, hoodies ou joggers";

        if (lc.matches(".*\\b(entrevista|interview|reunião|reuniao|meeting|conferência|conferencia|apresentação|apresentacao|trabalho|work|negócio|negocio|business)\\b.*"))
            return "PROFISSIONAL / BUSINESS — camisa, blazer ou casaco estruturado, calças formais; aspeto cuidado e neutro";

        if (lc.matches(".*\\b(praia|beach|piscina|pool|verão|verao|summer|festival|outdoor|picnic)\\b.*"))
            return "CASUAL / VERÃO — roupa leve e respirável; adequado para exterior";

        if (lc.matches(".*\\b(ginásio|ginasio|gym|treino|treinar|workout|corrida|running|desporto|sport|futebol|football|yoga)\\b.*"))
            return "DESPORTIVO / ACTIVEWEAR — roupa adequada para exercício físico; sem roupa formal";

        if (lc.matches(".*\\b(festa|party|halloween|carnaval|carnival|costume)\\b.*"))
            return "FESTIVO — look expressivo e divertido; podes ser criativo";

        if (lc.matches(".*\\b(concerto|concert|show|espetáculo|espetaculo|teatro|theatre|cinema)\\b.*"))
            return "CASUAL-COOL — confortável mas estiloso; evita fato completo";

        return null; // no specific dress code — rely on event name context in prompt
    }

    private String buildPrompt(List<WardrobeItem> items, Map<Long, Double> ragScores, String styleWeights, WeatherData weather, String eventName, List<OutfitHistory> recentHistory, List<OutfitHistory> likedHistory) {
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

                if (ragScores != null && ragScores.containsKey(item.getId())) {
                    node.put("ragSemanticScore", ragScores.get(item.getId()));
                }

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

            String eventContext;
            if (eventName != null) {
                String dressCode = resolveEventDressCode(eventName);
                if (dressCode != null) {
                    eventContext = """

                            EVENTO: %s
                            DRESS CODE OBRIGATÓRIO: %s
                            ⚠️ REGRA CRÍTICA: o outfit DEVE respeitar este dress code.
                            As preferências de estilo do utilizador são SECUNDÁRIAS ao dress code do evento.
                            Se o utilizador prefere streetwear mas o evento exige formal, escolhe as peças mais formais disponíveis.
                            """.formatted(eventName, dressCode);
                } else {
                    eventContext = "\nEVENTO: " + eventName + "\nAdapta o outfit à temática e contexto deste evento como prioridade principal.\n";
                }
            } else {
                eventContext = "";
            }

            return """
                    És um stylist de moda pessoal. Com base nas preferências de estilo e no armário do utilizador, cria combinações de outfits.%s
                    %s
                    Preferências de estilo do utilizador (pesos de 0 a 1) — respeitar APENAS quando não há dress code de evento:
                    %s

                    Peças disponíveis no armário (ordenadas por relevância semântica):
                    %s

                    OUTFITS USADOS RECENTEMENTE (evita repetição exata se possível, mas TENS de gerar um outfit):
                    %s

                    OUTFITS FAVORITOS (usa como referência de estilo):
                    %s

                    Se o armário for demasiado limitado para evitar repetição, repete mas menciona na descrição que o utilizador devia adicionar mais variedade.

                    Cria exatamente 1 outfit completo. Regras:
                    - O outfit deve ter pelo menos uma parte de cima (tops ou jackets) e uma de baixo (bottoms) ou sapatos (shoes)
                    - As peças devem combinar em cor e estilo
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
