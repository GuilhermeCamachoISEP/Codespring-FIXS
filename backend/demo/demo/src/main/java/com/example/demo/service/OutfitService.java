package com.example.demo.service;

import com.example.demo.domain.UserPreferences;
import com.example.demo.domain.WardrobeItem;
import com.example.demo.dto.OutfitSuggestion;
import com.example.demo.dto.WeatherData;
import com.example.demo.repository.UserPreferencesRepository;
import com.example.demo.repository.WardrobeItemRepository;
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
    private final ClaudeService claudeService;
    private final ObjectMapper objectMapper;

    public OutfitService(WardrobeItemRepository wardrobeItemRepository,
                         UserPreferencesRepository preferencesRepository,
                         ClaudeService claudeService,
                         ObjectMapper objectMapper) {
        this.wardrobeItemRepository = wardrobeItemRepository;
        this.preferencesRepository = preferencesRepository;
        this.claudeService = claudeService;
        this.objectMapper = objectMapper;
    }

    public List<OutfitSuggestion> generateOutfits(Long userId, WeatherData weather) {
        List<WardrobeItem> items = wardrobeItemRepository.findByUserIdOrderByCreatedAtDesc(userId);
        if (items.isEmpty()) return List.of();

        String styleWeights = preferencesRepository.findByUserId(userId)
                .map(UserPreferences::getStyleWeights)
                .orElse("{}");

        String prompt = buildPrompt(items, styleWeights, weather);
        String json = claudeService.generateOutfitsRaw(prompt);
        List<OutfitSuggestion> aiOutfits = parseOutfits(json, items);
        if (!aiOutfits.isEmpty()) {
            return aiOutfits;
        }
        return generateFallbackOutfits(items, styleWeights);
    }

    private String buildPrompt(List<WardrobeItem> items, String styleWeights, WeatherData weather) {
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

            return """
                    És um stylist de moda pessoal. Com base nas preferências de estilo e no armário do utilizador, cria combinações de outfits.
                    %s
                    Preferências de estilo do utilizador (pesos de 0 a 1):
                    %s

                    Peças disponíveis no armário:
                    %s

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
                    """.formatted(weatherContext, styleWeights, objectMapper.writeValueAsString(itemNodes));
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

        if (tops.isEmpty() && jackets.isEmpty()) return List.of();
        if (bottoms.isEmpty() && shoes.isEmpty()) return List.of();

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

    private String safeText(String value) {
        return value == null || value.isBlank() ? "peça" : value;
    }
}
