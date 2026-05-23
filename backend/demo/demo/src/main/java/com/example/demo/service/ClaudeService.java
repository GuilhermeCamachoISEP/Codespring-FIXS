package com.example.demo.service;

import com.example.demo.dto.ClothingMetadata;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Base64;
import java.util.List;
import com.example.demo.dto.ChatRequest;

@Service
public class ClaudeService {

    private static final String API_URL = "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent?key=%s";

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.model}")
    private String model;

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper;

    public ClaudeService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public boolean isConfigured() {
        return apiKey != null
                && !apiKey.contains("...")
                && !"changeme".equals(apiKey)
                && !apiKey.contains("cola-a-tua-chave")
                && apiKey.length() > 24;
    }

    public String getModel() {
        return model;
    }

    public ClothingMetadata classifyClothing(byte[] imageBytes, String mediaType, String categoryHint) {
        if (!isConfigured()) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "AI API key missing. Define GEMINI_API_KEY before starting the backend."
            );
        }

        try {
            String base64 = Base64.getEncoder().encodeToString(imageBytes);
            String body = buildRequestBody(base64, mediaType, categoryHint);

            HttpResponse<String> response = executeWithRetryAndFallback(objectMapper.readTree(body));

            if (response.statusCode() != 200) {
                System.err.println("Gemini API error " + response.statusCode() + ": " + response.body());
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "AI classification failed: Gemini returned HTTP " + response.statusCode()
                );
            }

            String text = extractGeminiText(response.body());
            // Strip markdown code fences if present
            text = text.replaceAll("```json\\s*", "").replaceAll("```\\s*", "").trim();
            ClothingMetadata metadata = objectMapper.readValue(text, ClothingMetadata.class);
            validateMetadata(metadata);
            return metadata;

        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            System.err.println("Gemini classification failed: " + e.getMessage());
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "AI classification failed. Check backend logs for details."
            );
        }
    }

    private String buildRequestBody(String base64, String mediaType, String categoryHint) throws Exception {
        var imageData = objectMapper.createObjectNode()
                .put("mimeType", mediaType)
                .put("data", base64);

        var imageContent = objectMapper.createObjectNode();
        imageContent.set("inlineData", imageData);

        var textContent = objectMapper.createObjectNode()
                .put("text", CLASSIFICATION_PROMPT.formatted(normalizeCategory(categoryHint)));

        var partsArray = objectMapper.createArrayNode();
        partsArray.add(textContent);
        partsArray.add(imageContent);

        var content = objectMapper.createObjectNode()
                .put("role", "user");
        content.set("parts", partsArray);

        var contentsArray = objectMapper.createArrayNode();
        contentsArray.add(content);

        var generationConfig = objectMapper.createObjectNode()
                .put("temperature", 0.1)
                .put("maxOutputTokens", 512)
                .put("responseMimeType", "application/json");

        var requestBody = objectMapper.createObjectNode();
        requestBody.set("contents", contentsArray);
        requestBody.set("generationConfig", generationConfig);

        return objectMapper.writeValueAsString(requestBody);
    }

    public String generateOutfitsRaw(String prompt) {
        try {
            if (!isConfigured()) return "[]";

            var textContent = objectMapper.createObjectNode().put("text", prompt);

            var partsArray = objectMapper.createArrayNode();
            partsArray.add(textContent);

            var content = objectMapper.createObjectNode().put("role", "user");
            content.set("parts", partsArray);

            var contentsArray = objectMapper.createArrayNode();
            contentsArray.add(content);

            var generationConfig = objectMapper.createObjectNode()
                    .put("temperature", 0.4)
                    .put("maxOutputTokens", 2048)
                    .put("responseMimeType", "application/json");

            var requestBody = objectMapper.createObjectNode();
            requestBody.set("contents", contentsArray);
            requestBody.set("generationConfig", generationConfig);

            HttpResponse<String> response = executeWithRetryAndFallback(requestBody);

            if (response.statusCode() != 200) {
                System.err.println("Gemini outfit API error " + response.statusCode() + ": " + response.body());
                return "[]";
            }

            String text = extractGeminiText(response.body());
            text = text.replaceAll("```json\\s*", "").replaceAll("```\\s*", "").trim();
            return text;

        } catch (Exception e) {
            System.err.println("Gemini outfit generation failed: " + e.getMessage());
            return "[]";
        }
    }

    public String chat(String systemPrompt, List<ChatRequest.ChatMessage> history, String newMessage) {
        try {
            if (!isConfigured()) return "AI não configurada no backend.";

            var requestBody = objectMapper.createObjectNode();

            if (systemPrompt != null && !systemPrompt.isBlank()) {
                var sysInstruction = objectMapper.createObjectNode();
                var sysParts = objectMapper.createArrayNode();
                sysParts.add(objectMapper.createObjectNode().put("text", systemPrompt));
                sysInstruction.set("parts", sysParts);
                requestBody.set("systemInstruction", sysInstruction);
            }

            var contentsArray = objectMapper.createArrayNode();

            if (history != null) {
                for (var msg : history) {
                    var content = objectMapper.createObjectNode()
                            .put("role", msg.getRole() != null ? msg.getRole() : "user");
                    var partsArray = objectMapper.createArrayNode();
                    partsArray.add(objectMapper.createObjectNode().put("text", msg.getContent()));
                    content.set("parts", partsArray);
                    contentsArray.add(content);
                }
            }

            var newContent = objectMapper.createObjectNode().put("role", "user");
            var newPartsArray = objectMapper.createArrayNode();
            newPartsArray.add(objectMapper.createObjectNode().put("text", newMessage));
            newContent.set("parts", newPartsArray);
            contentsArray.add(newContent);

            requestBody.set("contents", contentsArray);

            var generationConfig = objectMapper.createObjectNode()
                    .put("temperature", 0.7)
                    .put("maxOutputTokens", 2048);
            
            requestBody.set("generationConfig", generationConfig);

            HttpResponse<String> response = executeWithRetryAndFallback(requestBody);

            if (response.statusCode() != 200) {
                System.err.println("Gemini chat API error " + response.statusCode() + ": " + response.body());
                return "Desculpa, ocorreu um erro ao contactar a IA.";
            }

            String text = extractGeminiText(response.body());
            return text;

        } catch (Exception e) {
            System.err.println("Gemini chat failed: " + e.getMessage());
            return "Desculpa, ocorreu um erro interno.";
        }
    }

    public String generateJson(String prompt) {
        try {
            if (!isConfigured()) return "[]";

            var textContent = objectMapper.createObjectNode().put("text", prompt);
            var partsArray = objectMapper.createArrayNode();
            partsArray.add(textContent);
            var content = objectMapper.createObjectNode().put("role", "user");
            content.set("parts", partsArray);
            var contentsArray = objectMapper.createArrayNode();
            contentsArray.add(content);

            var generationConfig = objectMapper.createObjectNode()
                    .put("temperature", 0.3)
                    .put("maxOutputTokens", 512)
                    .put("responseMimeType", "application/json");

            var requestBody = objectMapper.createObjectNode();
            requestBody.set("contents", contentsArray);
            requestBody.set("generationConfig", generationConfig);

            HttpResponse<String> response = executeWithRetryAndFallback(requestBody);

            if (response.statusCode() != 200) {
                System.err.println("Gemini generateJson error " + response.statusCode() + ": " + response.body());
                return "[]";
            }

            String text = extractGeminiText(response.body());
            return text.replaceAll("```json\\s*", "").replaceAll("```\\s*", "").trim();

        } catch (Exception e) {
            System.err.println("Gemini generateJson failed: " + e.getMessage());
            return "[]";
        }
    }

    private String geminiUrl() {
        return API_URL.formatted(model, apiKey);
    }

    private HttpResponse<String> executeWithRetryAndFallback(JsonNode requestBody) throws Exception {
        int maxRetries = 3;
        int attempt = 0;
        String currentModel = model;
        HttpResponse<String> lastResponse = null;

        while (attempt < maxRetries) {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL.formatted(currentModel, apiKey)))
                    .header("content-type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(requestBody)))
                    .build();

            lastResponse = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (lastResponse.statusCode() == 200) {
                return lastResponse;
            } else if (lastResponse.statusCode() == 503 || lastResponse.statusCode() == 429) {
                attempt++;
                System.err.println("Gemini overloaded (" + lastResponse.statusCode() + "). Retrying " + attempt + "/" + maxRetries);
                if (attempt == 2) {
                    currentModel = "gemini-1.5-flash-latest"; // Fallback to ultra-stable model
                    System.err.println("Falling back to stable model: " + currentModel);
                }
                Thread.sleep(1500L * attempt);
            } else {
                return lastResponse; // other errors like 400, 404 should fail fast
            }
        }
        return lastResponse;
    }

    private String extractGeminiText(String responseBody) throws Exception {
        JsonNode root = objectMapper.readTree(responseBody);
        JsonNode parts = root.path("candidates").path(0).path("content").path("parts");
        if (!parts.isArray() || parts.isEmpty()) {
            throw new IllegalArgumentException("Gemini response did not include text parts");
        }
        return parts.get(0).path("text").asText();
    }

    private String normalizeCategory(String categoryHint) {
        if (categoryHint == null) return "tops";
        return switch (categoryHint) {
            case "tops", "bottoms", "shoes", "jackets", "accessories" -> categoryHint;
            default -> "tops";
        };
    }

    private void validateMetadata(ClothingMetadata metadata) {
        if (metadata.getCategory() == null || metadata.getCategory().isBlank()) {
            throw new IllegalArgumentException("Missing clothing category");
        }
        if (metadata.getSubcategory() == null || metadata.getSubcategory().isBlank()) {
            throw new IllegalArgumentException("Missing clothing subcategory");
        }
        if (metadata.getColor() == null || metadata.getColor().isBlank()) {
            metadata.setColor("unknown");
        }
        if (metadata.getFit() == null || metadata.getFit().isBlank()) {
            metadata.setFit("regular");
        }
        if (metadata.getMaterial() == null || metadata.getMaterial().isBlank()) {
            metadata.setMaterial("unknown");
        }
        if (metadata.getBrand() == null || metadata.getBrand().isBlank()) {
            metadata.setBrand("unknown");
        }
        if (metadata.getSeason() == null) {
            metadata.setSeason(List.of());
        }
        if (metadata.getStyle_tags() == null) {
            metadata.setStyle_tags(List.of());
        }
    }

    private static final String CLASSIFICATION_PROMPT = """
            You are a fashion classifier. Analyze this clothing item and respond ONLY with valid JSON, no markdown:
            The user selected this likely category: "%s". Prefer it unless the image clearly proves another category.
            {
              "category": "tops|bottoms|shoes|jackets|accessories",
              "subcategory": "specific item name (hoodie, cargo pants, sneakers, etc.)",
              "color": "primary color in English",
              "fit": "oversized|regular|slim|skinny",
              "material": "cotton|denim|leather|wool|synthetic|etc",
              "brand": "brand name or unknown",
              "season": ["spring","summer","fall","winter"],
              "style_tags": ["streetwear","casual","formal","sporty","minimalist","vintage","luxury"]
            }
            """;
}
