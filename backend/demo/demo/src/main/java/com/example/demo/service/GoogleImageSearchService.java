package com.example.demo.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

@Service
public class GoogleImageSearchService {

    private static final String SEARCH_URL = "https://serpapi.com/search.json?engine=google_images&q=%s&api_key=%s";

    @Value("${serpapi.key:}")
    private String apiKey;

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper;

    public GoogleImageSearchService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String fetchImageUrl(String query) {
        try {
            if (apiKey == null || apiKey.isBlank() || apiKey.equals("YOUR_SERPAPI_KEY")) {
                System.err.println("SerpApi key is missing or invalid: " + apiKey);
                return null;
            }

            // Pesquisa de imagens de roupa limpa no Google Images via SerpApi
            String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
            String url = String.format(SEARCH_URL, encodedQuery, apiKey);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonNode root = objectMapper.readTree(response.body());
                JsonNode imagesResults = root.path("images_results");
                if (imagesResults.isArray() && !imagesResults.isEmpty()) {
                    String imgUrl = imagesResults.get(0).path("original").asText();
                    System.out.println("SerpApi encontrou imagem: " + imgUrl);
                    return imgUrl;
                }
            } else {
                System.err.println("SerpApi error: " + response.statusCode() + " " + response.body());
            }
        } catch (Exception e) {
            System.err.println("Failed to fetch image from SerpApi: " + e.getMessage());
        }
        return null;
    }
}
