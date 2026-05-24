package com.example.demo.service;

import com.example.demo.domain.WardrobeItem;
import com.example.demo.dto.DayPlan;
import com.example.demo.dto.PackedItem;
import com.example.demo.dto.PackingResponse;
import com.example.demo.repository.WardrobeItemRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class PackingService {

    private final WardrobeItemRepository wardrobeItemRepository;
    private final ClaudeService claudeService;
    private final ObjectMapper objectMapper;

    public PackingService(WardrobeItemRepository wardrobeItemRepository, ClaudeService claudeService, ObjectMapper objectMapper) {
        this.wardrobeItemRepository = wardrobeItemRepository;
        this.claudeService = claudeService;
        this.objectMapper = objectMapper;
    }

    public PackingResponse generatePackingList(Long userId, String tripDescription, String travelDate) {
        List<WardrobeItem> items = wardrobeItemRepository.findByUserIdOrderByCreatedAtDesc(userId);
        
        if (items.size() < 5) {
            throw new IllegalArgumentException("O teu armário tem poucas peças para planear uma viagem. Adiciona mais roupa pelo chat antes de usar esta funcionalidade.");
        }

        String wardrobeContext = buildWardrobeContext(items);
        
        String prompt = """
            You are a professional travel stylist and packing optimizer.
            You will receive a user's complete wardrobe and a trip description.
            Your goal is to select the MINIMUM number of items that create the MAXIMUM number of distinct appropriate outfits for each day and context.
            
            Prioritize: versatile neutral pieces, items that work across multiple contexts, layering possibilities for cold weather.
            Avoid: packing separate items for each day when one item can serve multiple days, overpacking, recommending items that don't exist in the wardrobe.
            
            You must ONLY recommend items that exist in the wardrobe list provided. Never invent items.
            If the wardrobe lacks something critical, mention it in aiReasoning as a gentle suggestion.
            
            TRIP DESCRIPTION:
            %s
            
            TRAVEL DATE:
            %s
            (Please infer the typical weather for the destination during this time of year and pack accordingly)
            
            USER'S WARDROBE (DO NOT invent items outside this list!):
            %s
            
            Respond ONLY with a valid JSON object matching this structure (no markdown blocks, no text outside JSON):
            {
              "dayPlans": [
                {
                  "dayNumber": 1,
                  "context": "Dia de trabalho",
                  "items": [1, 5, 12],
                  "outfitDescription": "Blazer cinza com camisa branca"
                }
              ],
              "aiReasoning": "Escolhi o blazer cinza porque funciona bem tanto para o trabalho como para o jantar formal."
            }
            """.formatted(tripDescription, travelDate != null && !travelDate.isBlank() ? travelDate : "Não especificada", wardrobeContext);

        try {
            String jsonRaw = claudeService.generatePackingRaw(prompt);
            System.out.println("[DEBUG-PACKING] Raw AI Response: " + jsonRaw);
            
            // Clean up possible markdown
            String json = jsonRaw;
            if (jsonRaw.contains("```json")) {
                json = jsonRaw.split("```json")[1].split("```")[0].trim();
            } else if (jsonRaw.contains("```")) {
                json = jsonRaw.split("```")[1].split("```")[0].trim();
            } else {
                int start = jsonRaw.indexOf("{");
                int end = jsonRaw.lastIndexOf("}");
                if (start >= 0 && end > start) {
                    json = jsonRaw.substring(start, end + 1);
                }
            }

            JsonNode root = objectMapper.readTree(json);
            return parsePackingResponse(root, items);

        } catch (Exception e) {
            System.err.println("[DEBUG-PACKING] Error generating packing list: " + e.getMessage());
            return generateFallbackPacking(items);
        }
    }

    private PackingResponse parsePackingResponse(JsonNode root, List<WardrobeItem> items) {
        Map<Long, WardrobeItem> wardrobeMap = items.stream()
                .collect(Collectors.toMap(WardrobeItem::getId, item -> item));

        List<DayPlan> dayPlans = new ArrayList<>();
        Map<Long, Integer> itemUsageCount = new HashMap<>();

        JsonNode plansNode = root.path("dayPlans");
        if (plansNode.isArray()) {
            for (JsonNode planNode : plansNode) {
                int dayNumber = planNode.path("dayNumber").asInt();
                String context = planNode.path("context").asText();
                String desc = planNode.path("outfitDescription").asText();
                
                List<Long> outfitItems = new ArrayList<>();
                for (JsonNode idNode : planNode.path("items")) {
                    Long itemId = idNode.asLong();
                    // Hallucination Guard: Only add if item actually exists in the wardrobe
                    if (wardrobeMap.containsKey(itemId)) {
                        outfitItems.add(itemId);
                        itemUsageCount.put(itemId, itemUsageCount.getOrDefault(itemId, 0) + 1);
                    }
                }
                
                if (!outfitItems.isEmpty()) {
                    dayPlans.add(new DayPlan(dayNumber, context, outfitItems, desc));
                }
            }
        }

        List<PackedItem> packedItems = new ArrayList<>();
        for (Map.Entry<Long, Integer> entry : itemUsageCount.entrySet()) {
            WardrobeItem wi = wardrobeMap.get(entry.getKey());
            // Versatility Score: 1 to 5 stars depending on usage count
            int score = Math.min(5, Math.max(1, entry.getValue()));
            packedItems.add(new PackedItem(wi.getId(), wi.getSubcategory(), wi.getImageUrl(), score, wi.getCategory()));
        }
        
        // Sort packing list by versatility descending
        packedItems.sort((a, b) -> Integer.compare(b.getVersatilityScore(), a.getVersatilityScore()));

        String reasoning = root.path("aiReasoning").asText("Plano otimizado para a tua viagem baseado no teu armário.");
        
        return new PackingResponse(packedItems, dayPlans, packedItems.size(), reasoning);
    }

    private String buildWardrobeContext(List<WardrobeItem> items) {
        StringBuilder sb = new StringBuilder();
        for (WardrobeItem item : items) {
            sb.append("ID: ").append(item.getId())
              .append(" | Categoria: ").append(item.getCategory())
              .append(" | Nome: ").append(item.getSubcategory())
              .append(" | Cor: ").append(item.getColor())
              .append(" | Tags: ").append(item.getStyleTags() != null ? item.getStyleTags() : "[]")
              .append("\n");
        }
        return sb.toString();
    }

    private PackingResponse generateFallbackPacking(List<WardrobeItem> items) {
        List<PackedItem> packingList = new ArrayList<>();
        List<DayPlan> dayPlans = new ArrayList<>();
        
        // Pick up to 7 random items as fallback
        for (int i = 0; i < Math.min(7, items.size()); i++) {
            WardrobeItem wi = items.get(i);
            packingList.add(new PackedItem(wi.getId(), wi.getSubcategory(), wi.getImageUrl(), 3, wi.getCategory()));
        }

        dayPlans.add(new DayPlan(1, "Dia Livre", packingList.stream().map(PackedItem::getItemId).collect(Collectors.toList()), "Combinação básica para dia de viagem. (A IA está indisponível de momento)"));
        
        return new PackingResponse(packingList, dayPlans, packingList.size(), "Ocorreu um erro no planeador IA. Sugerimos estas peças como base para a tua mala.");
    }
}
