package com.example.demo.service;

import com.example.demo.domain.UserPreferences;
import com.example.demo.domain.WardrobeItem;
import com.example.demo.dto.ChatRequest;
import com.example.demo.dto.ChatResponse;
import com.example.demo.dto.WeatherData;
import com.example.demo.repository.UserPreferencesRepository;
import com.example.demo.repository.WardrobeItemRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ChatService {

    private final WardrobeItemRepository wardrobeItemRepository;
    private final UserPreferencesRepository preferencesRepository;
    private final ClaudeService claudeService;
    private final WeatherService weatherService;
    private final GoogleImageSearchService googleImageSearchService;
    private final ObjectMapper objectMapper;

    public ChatService(WardrobeItemRepository wardrobeItemRepository,
                       UserPreferencesRepository preferencesRepository,
                       ClaudeService claudeService,
                       WeatherService weatherService,
                       GoogleImageSearchService googleImageSearchService,
                       ObjectMapper objectMapper) {
        this.wardrobeItemRepository = wardrobeItemRepository;
        this.preferencesRepository = preferencesRepository;
        this.claudeService = claudeService;
        this.weatherService = weatherService;
        this.googleImageSearchService = googleImageSearchService;
        this.objectMapper = objectMapper;
    }

    public ChatResponse processChat(Long userId, ChatRequest request) {
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
        
        response = extractAndSaveItems(userId, response);
        System.out.println("[DEBUG-CHAT] Cleaned response to user:\n" + response);
        
        return new ChatResponse(response);
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
            for (JsonNode itemNode : node.get("confirmed")) {
                String itemName = itemNode.isObject() ? (itemNode.has("name") ? itemNode.get("name").asText() : itemNode.toString()) : itemNode.asText();
                if (itemName == null || itemName.trim().isEmpty()) {
                    System.out.println("[DEBUG-CHAT] Skipping empty itemName (likely JSON formatting error)");
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
                
                WardrobeItem newItem = WardrobeItem.builder()
                        .userId(userId)
                        .category("tops") // generic fallback
                        .subcategory(itemName)
                        .color("")
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
        
        String modeStr = (mode != null && mode.equals("conversational")) ? "add_clothes_mode" : "photo_mode";
        
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
                Se o utilizador mencionar peças que quer adicionar (ex: "comprei uma t-shirt preta, um cachecol e umas meias"), tens OBRIGATORIAMENTE de devolver um bloco JSON no final da mensagem para guardar TODAS as peças no array "confirmed". Podes e deves guardar múltiplas peças ao mesmo tempo!
                Se o utilizador disser que se enganou ou quiser corrigir uma peça anterior (ex: "não era azul, era vermelha"), usa o campo "deleted" com o ID numérico exato da peça antiga (que está no teu context 'known_wardrobe') e o "confirmed" com o nome da peça nova.

                Exemplo do formato JSON obrigatório (usa sempre os ```json):
                ```json
                {
                  "confirmed": ["t-shirt preta", "cachecol", "meias"],
                  "deleted": [102]
                }
                ```
                
                NEVER:
                - Suggest outfits in add_clothes_mode
                - Mention the mode name to the user
                """.formatted(weatherStr, calendarEvent, modeStr, itemsStr.toString(), recentOutfits, colorPalette, styleWeights);
    }
}
