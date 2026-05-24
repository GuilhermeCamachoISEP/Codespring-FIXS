package com.example.demo.service;

import com.example.demo.domain.UserPreferences;
import com.example.demo.domain.WardrobeItem;
import com.example.demo.dto.ChatRequest;
import com.example.demo.dto.ChatResponse;
import com.example.demo.dto.OutfitSuggestion;
import com.example.demo.dto.WeatherData;
import com.example.demo.repository.OutfitReservationRepository;
import com.example.demo.repository.UserPreferencesRepository;
import com.example.demo.repository.WardrobeItemRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ChatService {

    private final WardrobeItemRepository wardrobeItemRepository;
    private final OutfitReservationRepository outfitReservationRepository;
    private final UserPreferencesRepository preferencesRepository;
    private final ClaudeService claudeService;
    private final WeatherService weatherService;
    private final GoogleImageSearchService googleImageSearchService;
    private final OutfitService outfitService;
    private final ObjectMapper objectMapper;

    public ChatService(WardrobeItemRepository wardrobeItemRepository,
                       OutfitReservationRepository outfitReservationRepository,
                       UserPreferencesRepository preferencesRepository,
                       ClaudeService claudeService,
                       WeatherService weatherService,
                       GoogleImageSearchService googleImageSearchService,
                       OutfitService outfitService,
                       ObjectMapper objectMapper) {
        this.wardrobeItemRepository = wardrobeItemRepository;
        this.outfitReservationRepository = outfitReservationRepository;
        this.preferencesRepository = preferencesRepository;
        this.claudeService = claudeService;
        this.weatherService = weatherService;
        this.googleImageSearchService = googleImageSearchService;
        this.outfitService = outfitService;
        this.objectMapper = objectMapper;
    }

    public ChatResponse processChat(Long userId, ChatRequest request) {
        if ("outfit-refine".equals(request.getMode())) {
            return processOutfitRefine(userId, request);
        }

        List<WardrobeItem> items = wardrobeItemRepository.findByUserIdOrderByCreatedAtDesc(userId);

        String styleWeights = preferencesRepository.findByUserId(userId)
                .map(UserPreferences::getStyleWeights)
                .orElse("{}");

        WeatherData weather = null;
        if (request.getLat() != null && request.getLon() != null) {
            weather = weatherService.getWeather(request.getLat(), request.getLon());
        }

        String systemPrompt = buildSystemPrompt(items, styleWeights, weather, request.getMode());
        System.out.println("[DEBUG-CHAT] Sending chat request to ClaudeService...");
        String response = claudeService.chat(systemPrompt, request.getHistory(), request.getMessage());
        System.out.println("[DEBUG-CHAT] Raw response from AI:\n" + response);

        // Surface Groq infrastructure errors directly instead of showing raw error markers
        if (ClaudeService.isGroqError(response)) {
            return new ChatResponse(ClaudeService.groqErrorMessage(response), null);
        }

        response = extractAndSaveItems(userId, response);
        System.out.println("[DEBUG-CHAT] Cleaned response to user:\n" + response);

        return new ChatResponse(response, null);
    }

    private ChatResponse processOutfitRefine(Long userId, ChatRequest request) {
        List<WardrobeItem> wardrobe = resolveWardrobeForRefine(userId, request);
        if (wardrobe.isEmpty()) {
            return new ChatResponse("O teu armário está vazio. Adiciona peças antes de refinar.", List.of());
        }

        OutfitSuggestion currentOutfit = request.getContext() != null
                ? request.getContext().getCurrentOutfit()
                : null;
        if (currentOutfit == null) {
            return new ChatResponse("Não foi possível obter o outfit atual.", List.of());
        }

        String styleWeights = preferencesRepository.findByUserId(userId)
                .map(UserPreferences::getStyleWeights)
                .orElse("{}");

        WeatherData weather = null;
        if (request.getLat() != null && request.getLon() != null) {
            try {
                weather = weatherService.getWeather(request.getLat(), request.getLon());
            } catch (Exception e) {
                System.err.println("[DEBUG-CHAT] Weather fetch failed for refine: " + e.getMessage());
            }
        }

        String systemPrompt = buildOutfitRefinePrompt(currentOutfit, wardrobe, styleWeights, weather, request.getHistory());
        System.out.println("[DEBUG-CHAT] Sending outfit-refine request to Groq...");
        String raw = claudeService.chat(systemPrompt, request.getHistory(), request.getMessage());
        System.out.println("[DEBUG-CHAT] Raw outfit-refine response:\n" + raw);

        // Surface Groq infrastructure errors directly to the user
        if (ClaudeService.isGroqError(raw)) {
            return new ChatResponse(ClaudeService.groqErrorMessage(raw), List.of());
        }

        String jsonPayload = extractOutfitJsonPayload(raw);
        if (!jsonPayload.trim().startsWith("[")) {
            jsonPayload = "[" + jsonPayload + "]";
        }
        List<OutfitSuggestion> refined = outfitService.parseOutfitSuggestions(jsonPayload, wardrobe);
        if (refined.isEmpty()) {
            // The AI responded but not with valid outfit JSON — show what it actually said
            String fallbackMsg = (raw != null && !raw.isBlank()
                    && !raw.trim().startsWith("[") && !raw.trim().startsWith("{"))
                    ? raw   // plain-text AI response — show it
                    : "Não consegui aplicar o refinamento. Tenta reformular a instrução.";
            return new ChatResponse(fallbackMsg, List.of());
        }

        OutfitSuggestion merged = mergeRefinedOutfit(currentOutfit, refined.get(0), request.getMessage());
        return new ChatResponse("", List.of(merged));
    }

    private List<WardrobeItem> resolveWardrobeForRefine(Long userId, ChatRequest request) {
        List<WardrobeItem> fromDb = wardrobeItemRepository.findByUserIdOrderByCreatedAtDesc(userId);
        if (request.getContext() == null || request.getContext().getWardrobeItems() == null
                || request.getContext().getWardrobeItems().isEmpty()) {
            return fromDb;
        }
        return fromDb;
    }

    private String extractOutfitJsonPayload(String response) {
        if (response == null || response.isBlank()) {
            return "[]";
        }
        String trimmed = response.trim();
        if (trimmed.contains("```json")) {
            int start = trimmed.indexOf("```json") + 7;
            int end = trimmed.indexOf("```", start);
            if (end > start) {
                return trimmed.substring(start, end).trim();
            }
        }
        if (trimmed.contains("```")) {
            int start = trimmed.indexOf("```") + 3;
            int end = trimmed.indexOf("```", start);
            if (end > start) {
                return trimmed.substring(start, end).trim();
            }
        }
        int arrayStart = trimmed.indexOf('[');
        int arrayEnd = trimmed.lastIndexOf(']');
        if (arrayStart >= 0 && arrayEnd > arrayStart) {
            return trimmed.substring(arrayStart, arrayEnd + 1);
        }
        return trimmed;
    }

    private String buildOutfitRefinePrompt(OutfitSuggestion currentOutfit,
                                           List<WardrobeItem> wardrobe,
                                           String styleWeights,
                                           WeatherData weather,
                                           List<ChatRequest.ChatMessage> history) {
        String weatherStr = weather != null
                ? String.format("%.1f°C, %s (%s)", weather.getTemperature(), weather.getDescription(), weather.getCity())
                : "Desconhecido";

        String currentOutfitJson;
        String wardrobeJson;
        String historyJson;
        try {
            var currentNode = objectMapper.createObjectNode()
                    .put("name", currentOutfit.getName())
                    .put("description", currentOutfit.getDescription() != null ? currentOutfit.getDescription() : "")
                    .put("weatherNote", currentOutfit.getWeatherNote() != null ? currentOutfit.getWeatherNote() : "");
            var itemIds = objectMapper.createArrayNode();
            var currentItemsDetail = objectMapper.createArrayNode();
            if (currentOutfit.getItems() != null) {
                for (WardrobeItem item : currentOutfit.getItems()) {
                    itemIds.add(item.getId());
                    currentItemsDetail.add(objectMapper.createObjectNode()
                            .put("id", item.getId())
                            .put("category", item.getCategory())
                            .put("subcategory", item.getSubcategory())
                            .put("color", item.getColor() != null ? item.getColor() : ""));
                }
            }
            currentNode.set("itemIds", itemIds);
            currentNode.set("items", currentItemsDetail);

            var wardrobeNodes = objectMapper.createArrayNode();
            for (WardrobeItem item : wardrobe) {
                wardrobeNodes.add(objectMapper.createObjectNode()
                        .put("id", item.getId())
                        .put("category", item.getCategory())
                        .put("subcategory", item.getSubcategory())
                        .put("color", item.getColor() != null ? item.getColor() : "")
                        .put("fit", item.getFit() != null ? item.getFit() : "")
                        .put("material", item.getMaterial() != null ? item.getMaterial() : ""));
            }

            var historyNodes = objectMapper.createArrayNode();
            if (history != null) {
                for (ChatRequest.ChatMessage msg : history) {
                    historyNodes.add(objectMapper.createObjectNode()
                            .put("role", msg.getRole())
                            .put("content", msg.getContent()));
                }
            }

            currentOutfitJson = objectMapper.writeValueAsString(currentNode);
            wardrobeJson = objectMapper.writeValueAsString(wardrobeNodes);
            historyJson = objectMapper.writeValueAsString(historyNodes);
        } catch (Exception e) {
            currentOutfitJson = "{}";
            wardrobeJson = "[]";
            historyJson = "[]";
        }

        return """
                És um stylist de moda pessoal na app Codespring-FIXS.
                O utilizador está a REFINAR o outfit do dia com instruções em linguagem natural.

                OUTFIT ATUAL (peças com id, nome, cor, categoria):
                %s

                ARMÁRIO DISPONÍVEL (usa APENAS estes IDs em itemIds):
                %s

                PREFERÊNCIAS DE ESTILO:
                %s

                CLIMA ATUAL:
                %s

                HISTÓRICO DE REFINAMENTOS DESTA SESSÃO (instruções anteriores):
                %s

                REGRAS CRÍTICAS (OBRIGATÓRIO):
                - PRINCÍPIO DE MUDANÇA MÍNIMA: começa SEMPRE com os itemIds do outfit atual e altera só o estritamente necessário.
                - Se o utilizador diz "adiciona/acrescenta/inclui" (ex: "adiciona um chapéu"): MANTÉM TODOS os itemIds atuais
                  e acrescenta APENAS a nova peça (tipicamente accessories). NÃO troques tops, bottoms, shoes ou jackets.
                - Se diz "muda/troca/substitui X": altera SOMENTE a categoria X; todas as outras categorias mantêm o mesmo ID.
                - Se diz "sem verde" ou filtro de cor: podes trocar só as peças que violam o pedido; o resto mantém o ID original.
                - NUNCA substituas uma peça que o utilizador não mencionou.
                - Usa SOMENTE IDs do armário disponível.
                - O outfit final deve ter topo (tops/jackets) e baixo (bottoms) ou sapatos (shoes).
                - Nome e descrição em português de Portugal.
                - weatherNote: nota curta (máx. 6 palavras), ou igual à atual se ainda aplicável.

                RESPONDE EXCLUSIVAMENTE com um JSON array válido com exatamente 1 objeto, SEM markdown, SEM texto antes ou depois:
                [
                  {
                    "name": "Nome do outfit",
                    "description": "Descrição curta",
                    "itemIds": [1, 4, 7],
                    "weatherNote": "Perfeito para este frio"
                  }
                ]
                """.formatted(currentOutfitJson, wardrobeJson, styleWeights, weatherStr, historyJson);
    }

    /**
     * Garante mudanças mínimas no servidor, mesmo quando o modelo troca peças a mais.
     */
    private OutfitSuggestion mergeRefinedOutfit(OutfitSuggestion current, OutfitSuggestion ai, String userMessage) {
        if (current.getItems() == null || current.getItems().isEmpty()) {
            return ai;
        }
        String msg = userMessage != null ? userMessage.toLowerCase() : "";
        Set<Long> currentIds = current.getItems().stream().map(WardrobeItem::getId).collect(Collectors.toSet());
        Set<String> mentionedCategories = detectMentionedCategories(msg);

        if (mentionedCategories.contains("__color_filter__") && !isAddIntent(msg)) {
            return new OutfitSuggestion(
                    ai.getName() != null ? ai.getName() : current.getName(),
                    ai.getDescription() != null ? ai.getDescription() : current.getDescription(),
                    ai.getItems(),
                    coalesceWeatherNote(current, ai)
            );
        }

        List<WardrobeItem> merged;

        if (isAddIntent(msg)) {
            merged = new ArrayList<>(current.getItems());
            for (WardrobeItem aiItem : ai.getItems()) {
                if (currentIds.contains(aiItem.getId())) {
                    continue;
                }
                String cat = normalizeCategory(aiItem.getCategory());
                if ("accessories".equals(cat) || mentionedCategories.contains(cat)) {
                    merged.add(aiItem);
                }
            }
        } else {
            Map<String, List<WardrobeItem>> currentByCat = groupByCategory(current.getItems());
            Map<String, List<WardrobeItem>> aiByCat = groupByCategory(ai.getItems());
            merged = new ArrayList<>();

            Set<String> allCats = new HashSet<>();
            allCats.addAll(currentByCat.keySet());
            allCats.addAll(aiByCat.keySet());

            for (String cat : allCats) {
                if ("__color_filter__".equals(cat)) continue;

                boolean shouldUseAi = mentionedCategories.contains(cat) || explicitlyReplaceCategory(msg, cat);

                if (shouldUseAi) {
                    List<WardrobeItem> fromAi = aiByCat.get(cat);
                    if (fromAi != null && !fromAi.isEmpty()) {
                        if (isSingleSlotCategory(cat)) {
                            merged.add(fromAi.get(0));
                        } else {
                            merged.addAll(fromAi);
                        }
                    } else {
                        merged.addAll(currentByCat.getOrDefault(cat, List.of()));
                    }
                } else {
                    merged.addAll(currentByCat.getOrDefault(cat, List.of()));
                }
            }

            // Peças novas da IA (ex: acessório) quando não é add explícito mas categoria foi mencionada
            for (WardrobeItem aiItem : ai.getItems()) {
                if (!currentIds.contains(aiItem.getId())
                        && merged.stream().noneMatch(i -> i.getId().equals(aiItem.getId()))) {
                    String cat = normalizeCategory(aiItem.getCategory());
                    if (mentionedCategories.contains(cat)) {
                        if (isSingleSlotCategory(cat)) {
                            merged.removeIf(i -> cat.equals(normalizeCategory(i.getCategory())));
                        }
                        merged.add(aiItem);
                    }
                }
            }
        }

        return new OutfitSuggestion(
                ai.getName() != null ? ai.getName() : current.getName(),
                ai.getDescription() != null ? ai.getDescription() : current.getDescription(),
                merged,
                coalesceWeatherNote(current, ai)
        );
    }

    private String coalesceWeatherNote(OutfitSuggestion current, OutfitSuggestion ai) {
        if (ai.getWeatherNote() != null && !ai.getWeatherNote().isBlank()) {
            return ai.getWeatherNote();
        }
        return current.getWeatherNote();
    }

    private boolean isAddIntent(String msg) {
        return msg.matches(".*\\b(adiciona|adicionar|acrescenta|acrescentar|inclui|incluir|põe|poe|ponha|coloca|colocar)\\b.*");
    }

    private boolean explicitlyReplaceCategory(String msg, String category) {
        if ("accessories".equals(category)) {
            return msg.matches(".*\\b(muda|mudar|troca|trocar|substitui|substituir)\\b.*(chapéu|chapeu|boné|bone|acessório|acessorio|cachecol|óculos|oculos|relógio|relogio)\\b.*")
                    || msg.matches(".*\\b(chapéu|chapeu|boné|bone|acessório|acessorio)\\b.*\\b(muda|mudar|troca|trocar)\\b.*");
        }
        return switch (category) {
            case "tops" -> msg.matches(".*\\b(muda|mudar|troca|trocar|substitui|substituir)\\b.*(t-shirt|tshirt|camisa|top|blusa|polo|hoodie|sweatshirt|camisola)\\b.*")
                    || msg.matches(".*(t-shirt|tshirt|camisa|top|blusa|polo)\\b.*\\b(muda|mudar|troca|trocar)\\b.*");
            case "bottoms" -> msg.matches(".*\\b(muda|mudar|troca|trocar)\\b.*(calças|calcas|jeans|calção|calcoes|saia|shorts)\\b.*");
            case "shoes" -> msg.matches(".*\\b(muda|mudar|troca|trocar)\\b.*(sapatos|sapato|ténis|tenis|botas|sneakers)\\b.*");
            case "jackets" -> msg.matches(".*\\b(muda|mudar|troca|trocar)\\b.*(casaco|blusão|blusao|jacket)\\b.*");
            default -> false;
        };
    }

    private Set<String> detectMentionedCategories(String msg) {
        Set<String> cats = new HashSet<>();
        if (msg.matches(".*\\b(chapéu|chapeu|boné|bone|gorro|beanie|cachecol|óculos|oculos|relógio|relogio|acessório|acessorio|hat|cap)\\b.*")) {
            cats.add("accessories");
        }
        if (msg.matches(".*\\b(t-shirt|tshirt|camisa|top|blusa|polo|hoodie|sweatshirt|camisola)\\b.*")) {
            cats.add("tops");
        }
        if (msg.matches(".*\\b(calças|calcas|jeans|calção|calcoes|saia|shorts|fato de banho)\\b.*")) {
            cats.add("bottoms");
        }
        if (msg.matches(".*\\b(sapatos|sapato|ténis|tenis|botas|sneakers|sandálias|sandalias)\\b.*")) {
            cats.add("shoes");
        }
        if (msg.matches(".*\\b(casaco|blusão|blusao|jacket|puffer|bomber)\\b.*")) {
            cats.add("jackets");
        }
        if (msg.matches(".*\\b(verde|azul|vermelho|preto|branco|amarelo|rosa|cinzento|cor)\\b.*")) {
            cats.add("__color_filter__");
        }
        return cats;
    }

    private Map<String, List<WardrobeItem>> groupByCategory(List<WardrobeItem> items) {
        Map<String, List<WardrobeItem>> map = new LinkedHashMap<>();
        for (WardrobeItem item : items) {
            String cat = normalizeCategory(item.getCategory());
            map.computeIfAbsent(cat, k -> new ArrayList<>()).add(item);
        }
        return map;
    }

    private boolean sameItemIds(List<WardrobeItem> a, List<WardrobeItem> b) {
        if (a == null || b == null) return false;
        Set<Long> idsA = a.stream().map(WardrobeItem::getId).collect(Collectors.toSet());
        Set<Long> idsB = b.stream().map(WardrobeItem::getId).collect(Collectors.toSet());
        return idsA.equals(idsB);
    }

    private String normalizeCategory(String category) {
        if (category == null) return "tops";
        return switch (category.toLowerCase()) {
            case "top" -> "tops";
            case "bottom", "pants", "trousers" -> "bottoms";
            case "shoe", "footwear" -> "shoes";
            case "jacket", "coat", "outerwear" -> "jackets";
            case "accessory", "hat", "hats" -> "accessories";
            default -> category.toLowerCase();
        };
    }

    private boolean isSingleSlotCategory(String category) {
        return !"accessories".equals(category);
    }

    private String extractAndSaveItems(Long userId, String response) {
        System.out.println("[DEBUG-CHAT] Entering extractAndSaveItems...");
        try {
            if (response.contains("```json")) {
                System.out.println("[DEBUG-CHAT] Found ```json block");
                int start = response.indexOf("```json") + 7;
                int end = response.indexOf("```", start);
                if (end > start) {
                    String jsonStr = response.substring(start, end).trim();
                    System.out.println("[DEBUG-CHAT] Extracted JSON string: " + jsonStr);
                    JsonNode node = objectMapper.readTree(jsonStr);
                    saveConfirmedItems(userId, node);
                    return response.substring(0, response.indexOf("```json")).trim();
                }
            } else if (response.contains("{\"confirmed\"") || response.contains("{\"deleted\"")) {
                System.out.println("[DEBUG-CHAT] Found raw JSON block without markdown");
                int start = response.indexOf("{");
                int end = response.lastIndexOf("}") + 1;
                if (end > start) {
                    String jsonStr = response.substring(start, end).trim();
                    System.out.println("[DEBUG-CHAT] Extracted raw JSON string: " + jsonStr);
                    JsonNode node = objectMapper.readTree(jsonStr);
                    saveConfirmedItems(userId, node);
                    return response.substring(0, start).trim();
                }
            } else {
                System.out.println("[DEBUG-CHAT] No JSON or {\"confirmed\" signature found in response!");
            }
        } catch (Exception e) {
            System.err.println("[DEBUG-CHAT] Failed to parse confirmed items: " + e.getMessage());
            e.printStackTrace();
        }
        return response;
    }

    private void saveConfirmedItems(Long userId, JsonNode node) {
        System.out.println("[DEBUG-CHAT] Entering saveConfirmedItems with node: " + node.toString());
        
        // Handling deletes first (by ID)
        if (node.has("deleted") && node.get("deleted").isArray()) {
            System.out.println("[DEBUG-CHAT] Found " + node.get("deleted").size() + " deleted items.");
            for (JsonNode itemNode : node.get("deleted")) {
                try {
                    Long idToDel = itemNode.asLong();
                    System.out.println("[DEBUG-CHAT] Attempting to delete ID: " + idToDel);
                    wardrobeItemRepository.findById(idToDel).ifPresent(item -> {
                        if (item.getUserId().equals(userId)) {
                            outfitReservationRepository.deleteByWardrobeItemId(idToDel);
                            wardrobeItemRepository.delete(item);
                            System.out.println("[DEBUG-CHAT] Successfully deleted item ID: " + idToDel);
                        }
                    });
                } catch (Exception e) {
                    System.out.println("[DEBUG-CHAT] Failed to delete item, invalid ID format");
                }
            }
        }
        
        // Handling confirms
        if (node.has("confirmed") && node.get("confirmed").isArray()) {
            System.out.println("[DEBUG-CHAT] Found " + node.get("confirmed").size() + " confirmed items.");
            // Deduplication window: ignore items already added in the last 5 minutes
            java.time.LocalDateTime recentCutoff = java.time.LocalDateTime.now().minusMinutes(5);
            for (JsonNode itemNode : node.get("confirmed")) {
                String itemName = itemNode.isObject() ? (itemNode.has("name") ? itemNode.get("name").asText() : itemNode.toString()) : itemNode.asText();
                if (itemName == null || itemName.trim().isEmpty()) {
                    System.out.println("[DEBUG-CHAT] Skipping empty itemName (likely JSON formatting error)");
                    continue;
                }
                String category = itemNode.isObject() && itemNode.has("category") ? itemNode.get("category").asText() : "tops";
                // Guard: skip if the same item was already saved in the last 5 minutes (double-send protection)
                if (wardrobeItemRepository.existsRecentDuplicate(userId, itemName, category, recentCutoff)) {
                    System.out.println("[DEBUG-CHAT] Skipping duplicate (added <5min ago): " + itemName);
                    continue;
                }
                System.out.println("[DEBUG-CHAT] Processing item: " + itemName);
                String imageUrl = googleImageSearchService.fetchImageUrl(itemName);
                if (imageUrl == null) {
                    System.out.println("[DEBUG-CHAT] SerpApi failed/returned null, generating placeholder for " + itemName);
                    try {
                        imageUrl = "https://placehold.co/400x500/1e1e1e/fff?text=" + java.net.URLEncoder.encode(itemName, "UTF-8");
                    } catch (Exception e) {
                        imageUrl = "https://placehold.co/400x500/1e1e1e/fff?text=Roupa";
                    }
                } else {
                    System.out.println("[DEBUG-CHAT] SerpApi SUCCESS, url: " + imageUrl);
                }

                String color = itemNode.isObject() && itemNode.has("color") ? itemNode.get("color").asText() : "";
                
                WardrobeItem newItem = WardrobeItem.builder()
                        .userId(userId)
                        .category(category)
                        .subcategory(itemName)
                        .color(color)
                        .fit("regular")
                        .material("")
                        .brand("")
                        .imageUrl(imageUrl)
                        .season("[]")
                        .styleTags("[]")
                        .build();
                wardrobeItemRepository.save(newItem);
                System.out.println("[DEBUG-CHAT] Saved item to DB: " + itemName);
            }
        } else {
            System.out.println("[DEBUG-CHAT] No 'confirmed' array found in JSON node!");
        }
    }

    private String buildSystemPrompt(List<WardrobeItem> items, String styleWeights, WeatherData weather, String mode) {
        String weatherStr = weather != null ? String.format("%.1f°C, %s", weather.getTemperature(), weather.getDescription()) : "Desconhecido";
        String calendarEvent = "Sem eventos hoje"; 
        String colorPalette = "N/A";
        String recentOutfits = "N/A";
        
        String modeStr = "conversational".equals(mode) ? "add_clothes_mode" : "photo_mode";
        
        StringBuilder itemsStr = new StringBuilder();
        try {
            var itemNodes = objectMapper.createArrayNode();
            for (WardrobeItem item : items) {
                var node = objectMapper.createObjectNode()
                        .put("id", item.getId())
                        .put("category", item.getCategory())
                        .put("subcategory", item.getSubcategory())
                        .put("color", item.getColor());
                itemNodes.add(node);
            }
            itemsStr.append(objectMapper.writeValueAsString(itemNodes));
        } catch (Exception e) {}

        return """
                You are a personal stylist AI embedded in a wardrobe app called Codespring-FIXS.

                ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
                CONTEXT YOU ALWAYS RECEIVE:
                ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
                - weather: %s
                - calendar_event: "%s"
                - wardrobe_mode: "%s"
                - known_wardrobe: %s
                - recent_outfits: %s
                - user_color_palette: %s
                - user_style_preferences: %s

                ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
                WARDROBE MODES — BEHAVE DIFFERENTLY FOR EACH:
                ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

                MODE A — PHOTO MODE (high effort, high precision) [USE WHEN wardrobe_mode="photo_mode"]:
                User has uploaded photos of their wardrobe items.
                - You have real visual data: exact colors, cuts, textures
                - Only suggest outfits using confirmed wardrobe items
                - Be precise: "your olive cargo trousers" not "trousers"
                - When suggesting, reference the actual photo items by name/description
                - If a combination is visually imperfect, say why and suggest the fix:
                  "The olive cargos clash slightly with the burgundy shirt — 
                   swap for your white tee instead"
                - Confidence level on suggestions: HIGH — you've seen the clothes

                MODE B — ADD CLOTHES MODE (fast wardrobe building) [USE WHEN wardrobe_mode="add_clothes_mode"]:
                The user is chatting with you strictly to add new items to their virtual wardrobe.
                - DO NOT suggest outfits in this mode.
                - Your ONLY goal is to extract the clothing items the user mentions.
                - Be friendly, enthusiastic, and brief.
                - If they mention items, confirm you added them (e.g. "Boa! Adicionei a tua camisa preta. Mais alguma coisa?")
                - ALWAYS include the JSON block when they mention new items.

                ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
                UNIVERSAL RULES (BOTH MODES):
                ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
                - Always lead with one line of reasoning before the outfit:
                  "Reunião formal + 19°C → polished but layered"
                - One suggestion at a time. If user wants alternative, give ONE, not three
                - If user_color_palette is set, silently filter suggestions to 
                  flattering colors — never lecture about color theory unless asked
                - Never suggest buying new clothes
                - Never use filler: no "Great!", "Sure!", "Of course!", "Absolutely!"
                - MANTÉM TUDO EM PORTUGUÊS DE PORTUGAL. A tua resposta TEM DE SER na língua do utilizador.
                - MANTÉM TUDO EM PORTUGUÊS DE PORTUGAL. A tua resposta TEM DE SER na língua do utilizador.

                MUITO IMPORTANTE (ATUALIZAÇÃO DE ARMÁRIO):
                O mundo da moda é muito detalhado! Se o utilizador mencionar peças que quer adicionar mas a informação for vaga, NÃO devolvas o bloco JSON imediatamente. Tens de fazer perguntas de clarificação para teres a certeza absoluta de 3 coisas fundamentais antes de guardares:
                1. A Cor exata
                2. O Tipo/Corte exato (ex: calças cargo, t-shirt decote em V, casaco bomber, fit oversized vs slim)
                3. O Material (ex: ganga/denim, algodão, cabedal, linho)
                
                Exemplo: Se o utilizador disser "Comprei um casaco", tu perguntas "Boa! De que cor é? É de cabedal, ganga ou outro material? E é mais justo ou largo?".
                O teu objetivo é manter o contexto da conversa. Se eles responderem "preto de cabedal justo", tu lembras-te que estavam a falar de um casaco e avanças.
                
                Apenas quando tiveres a certeza do nome super detalhado (ex: "Casaco de cabedal preto slim"), categoria e cor, devolve OBRIGATORIAMENTE um bloco JSON no final da mensagem.
                Para cada peça nova em "confirmed", precisas de devolver um objeto com "name", "category" (tops, bottoms, shoes, jackets, accessories) e "color".
                
                Exemplo do formato JSON obrigatório (usa sempre os ```json e NUNCA o devolvas se ainda estiveres a fazer perguntas de clarificação):
                ```json
                {
                  "confirmed": [
                    {"name": "t-shirt básica", "category": "tops", "color": "preta"},
                    {"name": "sapatilhas", "category": "shoes", "color": "brancas"}
                  ],
                  "deleted": [102]
                }
                ```
                
                NEVER:
                - Suggest outfits in add_clothes_mode
                - Mention the mode name to the user
                """.formatted(weatherStr, calendarEvent, modeStr, itemsStr.toString(), recentOutfits, colorPalette, styleWeights);
    }
}
