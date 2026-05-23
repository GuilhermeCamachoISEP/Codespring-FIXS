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
        String response = claudeService.chat(systemPrompt, request.getHistory(), request.getMessage());
        
        response = extractAndSaveItems(userId, response);
        
        return new ChatResponse(response);
    }

    private String extractAndSaveItems(Long userId, String response) {
        try {
            if (response.contains("```json")) {
                int start = response.indexOf("```json") + 7;
                int end = response.indexOf("```", start);
                if (end > start) {
                    String jsonStr = response.substring(start, end).trim();
                    JsonNode node = objectMapper.readTree(jsonStr);
                    saveConfirmedItems(userId, node);
                    return response.substring(0, response.indexOf("```json")).trim();
                }
            } else if (response.contains("{\"confirmed\"")) {
                int start = response.indexOf("{\"confirmed\"");
                int end = response.indexOf("}", start) + 1;
                if (end > start) {
                    String jsonStr = response.substring(start, end).trim();
                    JsonNode node = objectMapper.readTree(jsonStr);
                    saveConfirmedItems(userId, node);
                    return response.replace(jsonStr, "").trim();
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to parse confirmed items: " + e.getMessage());
        }
        return response;
    }

    private void saveConfirmedItems(Long userId, JsonNode node) {
        if (node.has("confirmed") && node.get("confirmed").isArray()) {
            for (JsonNode itemNode : node.get("confirmed")) {
                String itemName = itemNode.asText();
                String imageUrl = googleImageSearchService.fetchImageUrl(itemName);
                if (imageUrl == null) {
                    imageUrl = "https://placehold.co/400x500/1e1e1e/fff?text=" + itemName.replace(" ", "+");
                }
                
                WardrobeItem newItem = WardrobeItem.builder()
                        .userId(userId)
                        .category("tops") // generic fallback
                        .subcategory(itemName)
                        .color("unknown")
                        .fit("regular")
                        .material("unknown")
                        .brand("unknown")
                        .imageUrl(imageUrl)
                        .season("[]")
                        .styleTags("[]")
                        .build();
                wardrobeItemRepository.save(newItem);
            }
        }
    }

    private String buildSystemPrompt(List<WardrobeItem> items, String styleWeights, WeatherData weather, String mode) {
        String weatherStr = weather != null ? String.format("%.1f°C, %s", weather.getTemperature(), weather.getDescription()) : "Desconhecido";
        String calendarEvent = "Sem eventos hoje"; 
        String colorPalette = "N/A";
        String recentOutfits = "N/A";
        
        String modeStr = (mode != null && mode.equals("conversational")) ? "conversational_mode" : "photo_mode";
        
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

                MODE B — CONVERSATIONAL MODE (zero friction, progressive) [USE WHEN wardrobe_mode="conversational_mode"]:
                User has no photos. Wardrobe builds through conversation.

                  IF wardrobe is completely empty (first use):
                  - Don't ask the user to set anything up
                  - Immediately suggest a full outfit based on weather + calendar context
                  - Use specific but generic items: "navy slim-fit chinos" not just "trousers"
                  - After suggesting, ask exactly one question:
                    "Do you have anything like this? Tell me what you own and 
                     I'll remember it for next time."
                  - Parse their response and return a wardrobe update JSON:
                    {"confirmed": ["item1", "item2"], "missing": ["item3"]}

                  IF wardrobe is partially built (returning user):
                  - Prioritize confirmed items in suggestions
                  - For gaps, suggest generic alternatives and ask if they own something similar
                  - Gradually fill wardrobe through natural conversation
                  - Never make the user feel like they're doing data entry

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
                
                ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
                RESPONSE FORMAT:
                ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
                Raciocínio: [uma linha — contexto + lógica]
                Look sugerido: [peça 1] + [peça 2] + [peça 3] (+ [peça 4] se necessário)
                Porque funciona: [uma linha — lógica de cores/estilo]
                Próximo passo: [uma pergunta curta ao utilizador para progredir]

                MUITO IMPORTANTE (ATUALIZAÇÃO DE ARMÁRIO):
                Se o utilizador acabar de CONFIRMAR que tem certas peças (ex: "sim, tenho umas calças de alfaiataria"), tens OBRIGATORIAMENTE de adicionar no final absoluto da tua mensagem, DEPOIS de todo o texto, um bloco JSON para o sistema guardar as peças dele automaticamente.
                Isto evita que o utilizador tenha trabalho manual!

                Exemplo do formato JSON obrigatório (usa sempre os ```json):
                ```json
                {
                  "confirmed": ["calças de alfaiataria", "camisa branca"],
                  "missing": []
                }
                ```
                
                NEVER:
                - Suggest items not in wardrobe when MODE A is active
                - Ask more than one question per response
                - Give generic advice disconnected from weather/calendar context
                - Mention the mode name to the user
                """.formatted(weatherStr, calendarEvent, modeStr, itemsStr.toString(), recentOutfits, colorPalette, styleWeights);
    }
}
