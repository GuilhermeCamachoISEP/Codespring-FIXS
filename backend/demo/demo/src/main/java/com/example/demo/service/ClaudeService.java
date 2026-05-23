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

@Service
public class ClaudeService {

    private static final String GROQ_URL = "https://api.groq.com/openai/v1/chat/completions";
    private static final String VISION_MODEL = "meta-llama/llama-4-scout-17b-16e-instruct";
    private static final String TEXT_MODEL = "llama-3.3-70b-versatile";

    @Value("${groq.api.key:}")
    private String apiKey;

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper;

    public ClaudeService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public boolean isConfigured() {
        return apiKey != null
                && !apiKey.isBlank()
                && !"changeme".equals(apiKey)
                && apiKey.length() > 20;
    }

    public String getModel() {
        return TEXT_MODEL;
    }

    // ─── Image classification ────────────────────────────────────────────────

    public ClothingMetadata classifyClothing(byte[] imageBytes, String mediaType, String categoryHint) {
        if (!isConfigured()) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "AI API key missing. Define GROQ_API_KEY before starting the backend."
            );
        }

        try {
            String base64 = Base64.getEncoder().encodeToString(imageBytes);
            String body = buildVisionRequestBody(base64, mediaType, categoryHint);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(GROQ_URL))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                System.err.println("Groq API error " + response.statusCode() + ": " + response.body());
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "AI classification failed: Groq returned HTTP " + response.statusCode()
                );
            }

            String text = extractGroqText(response.body());
            text = text.replaceAll("```json\\s*", "").replaceAll("```\\s*", "").trim();
            // Extract the JSON object in case the model adds extra prose
            int start = text.indexOf('{');
            int end   = text.lastIndexOf('}');
            if (start >= 0 && end > start) text = text.substring(start, end + 1);

            ClothingMetadata metadata = objectMapper.readValue(text, ClothingMetadata.class);
            validateMetadata(metadata);
            return metadata;

        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            System.err.println("Groq classification failed: " + e.getMessage());
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "AI classification failed. Check backend logs for details."
            );
        }
    }

    private String buildVisionRequestBody(String base64, String mediaType, String categoryHint) throws Exception {
        var textPart = objectMapper.createObjectNode()
                .put("type", "text")
                .put("text", CLASSIFICATION_PROMPT.formatted(normalizeCategory(categoryHint)));

        var imageUrlNode = objectMapper.createObjectNode()
                .put("url", "data:" + mediaType + ";base64," + base64);
        var imagePart = objectMapper.createObjectNode().put("type", "image_url");
        imagePart.set("image_url", imageUrlNode);

        var contentArray = objectMapper.createArrayNode();
        contentArray.add(textPart);
        contentArray.add(imagePart);

        var message = objectMapper.createObjectNode().put("role", "user");
        message.set("content", contentArray);

        var messagesArray = objectMapper.createArrayNode();
        messagesArray.add(message);

        var requestBody = objectMapper.createObjectNode()
                .put("model", VISION_MODEL)
                .put("temperature", 0.1)
                .put("max_tokens", 512);
        requestBody.set("messages", messagesArray);

        return objectMapper.writeValueAsString(requestBody);
    }

    // ─── Text generation ─────────────────────────────────────────────────────

    public String generateOutfitsRaw(String prompt) {
        try {
            if (!isConfigured()) return "[]";

            String body = buildTextRequestBody(prompt, 0.4, 2048);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(GROQ_URL))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                System.err.println("Groq outfit API error " + response.statusCode() + ": " + response.body());
                return "[]";
            }

            String text = extractGroqText(response.body());
            text = text.replaceAll("```json\\s*", "").replaceAll("```\\s*", "").trim();
            int start = text.indexOf('[');
            int end   = text.lastIndexOf(']');
            if (start >= 0 && end > start) text = text.substring(start, end + 1);
            return text;

        } catch (Exception e) {
            System.err.println("Groq outfit generation failed: " + e.getMessage());
            return "[]";
        }
    }

    public String chat(String systemPrompt, List<com.example.demo.dto.ChatRequest.ChatMessage> history, String userMessage) {
        try {
            if (!isConfigured()) return "AI is not configured.";

            var messagesArray = objectMapper.createArrayNode();

            var sysMsg = objectMapper.createObjectNode()
                    .put("role", "system")
                    .put("content", systemPrompt);
            messagesArray.add(sysMsg);

            if (history != null) {
                for (var msg : history) {
                    var node = objectMapper.createObjectNode()
                            .put("role", msg.getRole())
                            .put("content", msg.getContent());
                    messagesArray.add(node);
                }
            }

            var userMsg = objectMapper.createObjectNode()
                    .put("role", "user")
                    .put("content", userMessage);
            messagesArray.add(userMsg);

            var requestBody = objectMapper.createObjectNode()
                    .put("model", TEXT_MODEL)
                    .put("temperature", 0.4)
                    .put("max_tokens", 2048);
            requestBody.set("messages", messagesArray);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(GROQ_URL))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(requestBody)))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                System.err.println("Groq chat API error " + response.statusCode() + ": " + response.body());
                return "Desculpa, ocorreu um erro ao contactar a IA.";
            }

            return extractGroqText(response.body());

        } catch (Exception e) {
            System.err.println("Groq chat failed: " + e.getMessage());
            return "Desculpa, ocorreu um erro interno.";
        }
    }

    public String generateJson(String prompt) {
        try {
            if (!isConfigured()) return "[]";

            String body = buildTextRequestBody(prompt, 0.3, 512);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(GROQ_URL))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                System.err.println("Groq generateJson error " + response.statusCode() + ": " + response.body());
                return "[]";
            }

            String text = extractGroqText(response.body());
            text = text.replaceAll("```json\\s*", "").replaceAll("```\\s*", "").trim();
            // Support both array and object responses
            int startArr = text.indexOf('[');
            int startObj = text.indexOf('{');
            int start = (startArr >= 0 && startObj >= 0) ? Math.min(startArr, startObj)
                      : (startArr >= 0) ? startArr : startObj;
            int endArr  = text.lastIndexOf(']');
            int endObj  = text.lastIndexOf('}');
            int end = Math.max(endArr, endObj);
            if (start >= 0 && end > start) text = text.substring(start, end + 1);
            return text;

        } catch (Exception e) {
            System.err.println("Groq generateJson failed: " + e.getMessage());
            return "[]";
        }
    }

    private String buildTextRequestBody(String prompt, double temperature, int maxTokens) throws Exception {
        var message = objectMapper.createObjectNode()
                .put("role", "user")
                .put("content", prompt);

        var messagesArray = objectMapper.createArrayNode();
        messagesArray.add(message);

        var requestBody = objectMapper.createObjectNode()
                .put("model", TEXT_MODEL)
                .put("temperature", temperature)
                .put("max_tokens", maxTokens);
        requestBody.set("messages", messagesArray);

        return objectMapper.writeValueAsString(requestBody);
    }

    // ─── Response parsing ─────────────────────────────────────────────────────

    private String extractGroqText(String responseBody) throws Exception {
        JsonNode root = objectMapper.readTree(responseBody);
        JsonNode content = root.path("choices").path(0).path("message").path("content");
        if (content.isMissingNode()) {
            throw new IllegalArgumentException("Groq response missing content. Body: " + responseBody);
        }
        return content.asText();
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

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
            You are a fashion classifier. Analyze this clothing item image and respond ONLY with valid JSON — no markdown, no extra text, just the JSON object.
            The user selected this likely category: "%s". Prefer it unless the image clearly shows otherwise.
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
