package com.example.demo.service;

import com.example.demo.domain.UserPreferences;
import com.example.demo.dto.InspirationPhoto;
import com.example.demo.dto.InspirationSection;
import com.example.demo.repository.UserPreferencesRepository;
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
import java.util.*;

@Service
public class InspirationService {

    private static final String UNSPLASH_SEARCH = "https://api.unsplash.com/search/photos";

    // Maps internal style IDs → Unsplash search queries
    private static final Map<String, String> STYLE_QUERIES = Map.ofEntries(
            Map.entry("streetwear",   "streetwear outfit fashion aesthetic"),
            Map.entry("oldMoney",     "old money aesthetic fashion outfit"),
            Map.entry("gorpcore",     "gorpcore outdoor fashion outfit"),
            Map.entry("preppy",       "preppy style college fashion outfit"),
            Map.entry("vintage",      "vintage thrift fashion outfit aesthetic"),
            Map.entry("minimalist",   "minimalist outfit fashion neutral"),
            Map.entry("techwear",     "techwear outfit aesthetic futuristic"),
            Map.entry("y2k",          "y2k fashion outfit aesthetic 2000s"),
            Map.entry("luxury",       "luxury fashion outfit designer style"),
            Map.entry("darkAcademia", "dark academia aesthetic fashion outfit"),
            Map.entry("quietLuxury",  "quiet luxury fashion minimal outfit"),
            Map.entry("casual",       "casual chic outfit street style fashion")
    );

    private static final Map<String, String> STYLE_LABELS = Map.ofEntries(
            Map.entry("streetwear",   "Streetwear"),
            Map.entry("oldMoney",     "Old Money"),
            Map.entry("gorpcore",     "Gorpcore"),
            Map.entry("preppy",       "Preppy"),
            Map.entry("vintage",      "Vintage"),
            Map.entry("minimalist",   "Minimalista"),
            Map.entry("techwear",     "Techwear"),
            Map.entry("y2k",          "Y2K"),
            Map.entry("luxury",       "Luxury"),
            Map.entry("darkAcademia", "Dark Academia"),
            Map.entry("quietLuxury",  "Quiet Luxury"),
            Map.entry("casual",       "Casual")
    );

    @Value("${unsplash.access.key:}")
    private String accessKey;

    private final UserPreferencesRepository preferencesRepository;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    public InspirationService(UserPreferencesRepository preferencesRepository,
                              ObjectMapper objectMapper) {
        this.preferencesRepository = preferencesRepository;
        this.objectMapper = objectMapper;
    }

    public boolean isConfigured() {
        return accessKey != null && !accessKey.isBlank() && accessKey.length() > 10;
    }

    public List<InspirationSection> getInspirationForUser(Long userId) {
        List<String> topStyles = resolveTopStyles(userId);
        List<InspirationSection> sections = new ArrayList<>();

        for (String styleId : topStyles.subList(0, Math.min(3, topStyles.size()))) {
            String query = STYLE_QUERIES.getOrDefault(styleId, "fashion outfit aesthetic");
            String label = STYLE_LABELS.getOrDefault(styleId, styleId);
            try {
                List<InspirationPhoto> photos = fetchPhotos(query, 6, label);
                if (!photos.isEmpty()) {
                    sections.add(new InspirationSection(styleId, label, photos));
                }
            } catch (Exception e) {
                System.err.println("Unsplash fetch failed for style " + styleId + ": " + e.getMessage());
            }
        }
        return sections;
    }

    // ─── private helpers ──────────────────────────────────────────────────────

    private List<String> resolveTopStyles(Long userId) {
        try {
            String weightsJson = preferencesRepository.findByUserId(userId)
                    .map(UserPreferences::getStyleWeights)
                    .orElse("{}");

            JsonNode root = objectMapper.readTree(weightsJson);
            List<Map.Entry<String, Double>> entries = new ArrayList<>();
            root.fields().forEachRemaining(e ->
                    entries.add(Map.entry(e.getKey(), e.getValue().asDouble())));
            entries.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
            List<String> styles = entries.stream().map(Map.Entry::getKey).toList();
            return styles.isEmpty() ? List.of("minimalist", "casual") : styles;
        } catch (Exception e) {
            return List.of("minimalist", "casual");
        }
    }

    private List<InspirationPhoto> fetchPhotos(String query, int count, String styleLabel) throws Exception {
        String url = UNSPLASH_SEARCH
                + "?query=" + URLEncoder.encode(query, StandardCharsets.UTF_8)
                + "&per_page=" + count
                + "&orientation=portrait";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Client-ID " + accessKey)
                .header("Accept-Version", "v1")
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            System.err.println("Unsplash HTTP " + response.statusCode() + ": " + response.body());
            return List.of();
        }

        JsonNode root = objectMapper.readTree(response.body());
        List<InspirationPhoto> photos = new ArrayList<>();
        for (JsonNode result : root.path("results")) {
            photos.add(new InspirationPhoto(
                    result.path("id").asText(),
                    result.path("urls").path("regular").asText(),
                    result.path("urls").path("small").asText(),
                    result.path("links").path("html").asText(),
                    result.path("user").path("name").asText("Unknown"),
                    result.path("user").path("links").path("html").asText("#"),
                    styleLabel
            ));
        }
        return photos;
    }
}
