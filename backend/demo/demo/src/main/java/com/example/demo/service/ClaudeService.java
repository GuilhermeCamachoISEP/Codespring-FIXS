package com.example.demo.service;

import com.example.demo.dto.ClothingMetadata;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Base64;
import java.util.List;

@Service
public class ClaudeService {

    private static final String API_URL = "https://api.anthropic.com/v1/messages";

    @Value("${anthropic.api.key}")
    private String apiKey;

    @Value("${anthropic.model}")
    private String model;

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper;

    public ClaudeService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public ClothingMetadata classifyClothing(byte[] imageBytes, String mediaType) {
        try {
            String base64 = Base64.getEncoder().encodeToString(imageBytes);
            String body = buildRequestBody(base64, mediaType);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .header("x-api-key", apiKey)
                    .header("anthropic-version", "2023-06-01")
                    .header("content-type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                System.err.println("Claude API error " + response.statusCode() + ": " + response.body());
                return fallbackMetadata();
            }

            JsonNode root = objectMapper.readTree(response.body());
            String text = root.path("content").get(0).path("text").asText();
            // Strip markdown code fences if present
            text = text.replaceAll("```json\\s*", "").replaceAll("```\\s*", "").trim();
            return objectMapper.readValue(text, ClothingMetadata.class);

        } catch (Exception e) {
            System.err.println("Claude classification failed: " + e.getMessage());
            return fallbackMetadata();
        }
    }

    private String buildRequestBody(String base64, String mediaType) throws Exception {
        var imageSource = objectMapper.createObjectNode()
                .put("type", "base64")
                .put("media_type", mediaType)
                .put("data", base64);

        var imageContent = objectMapper.createObjectNode()
                .put("type", "image");
        imageContent.set("source", imageSource);

        var textContent = objectMapper.createObjectNode()
                .put("type", "text")
                .put("text", CLASSIFICATION_PROMPT);

        var messagesArray = objectMapper.createArrayNode();
        var userMessage = objectMapper.createObjectNode().put("role", "user");
        var contentArray = objectMapper.createArrayNode();
        contentArray.add(imageContent);
        contentArray.add(textContent);
        userMessage.set("content", contentArray);
        messagesArray.add(userMessage);

        var requestBody = objectMapper.createObjectNode()
                .put("model", model)
                .put("max_tokens", 512);
        requestBody.set("messages", messagesArray);

        return objectMapper.writeValueAsString(requestBody);
    }

    private ClothingMetadata fallbackMetadata() {
        ClothingMetadata m = new ClothingMetadata();
        m.setCategory("tops");
        m.setSubcategory("item");
        m.setColor("unknown");
        m.setFit("regular");
        m.setMaterial("unknown");
        m.setBrand("unknown");
        m.setSeason(List.of("spring", "summer", "fall", "winter"));
        m.setStyle_tags(List.of("casual"));
        return m;
    }

    private static final String CLASSIFICATION_PROMPT = """
            You are a fashion classifier. Analyze this clothing item and respond ONLY with valid JSON, no markdown:
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
