package com.example.demo.service;

import com.example.demo.domain.WardrobeItem;
import com.example.demo.dto.WeatherAdvisory;
import com.example.demo.dto.WeatherData;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class WeatherAdvisoryService {

    private final ClaudeService claudeService;
    private final ObjectMapper objectMapper;

    public WeatherAdvisoryService(ClaudeService claudeService, ObjectMapper objectMapper) {
        this.claudeService = claudeService;
        this.objectMapper = objectMapper;
    }

    public WeatherAdvisory generateAdvisory(WeatherData weather, List<WardrobeItem> wardrobe) {
        List<String> tips = generateTips(weather, wardrobe);
        String alert = detectWardrobeGap(weather, wardrobe);
        return new WeatherAdvisory(tips, alert);
    }

    private List<String> generateTips(WeatherData weather, List<WardrobeItem> wardrobe) {
        if (claudeService.isConfigured()) {
            try {
                String prompt = buildTipsPrompt(weather, wardrobe);
                String json = claudeService.generateJson(prompt);
                List<String> aiTips = parseTips(json);
                if (!aiTips.isEmpty()) return aiTips;
            } catch (Exception e) {
                System.err.println("AI weather tips failed, using rule-based: " + e.getMessage());
            }
        }
        return ruleBased(weather);
    }

    private String buildTipsPrompt(WeatherData weather, List<WardrobeItem> wardrobe) {
        Map<String, Long> categoryCounts = wardrobe.stream()
                .collect(Collectors.groupingBy(
                        item -> item.getCategory() != null ? item.getCategory() : "unknown",
                        Collectors.counting()
                ));

        StringBuilder wardrobeSummary = new StringBuilder();
        categoryCounts.forEach((cat, count) ->
                wardrobeSummary.append("- ").append(count).append(" ").append(cat).append("\n"));

        boolean rainy = weather.getWeatherCode() >= 51 && weather.getWeatherCode() <= 82;
        boolean windy = weather.getWindSpeed() > 30;

        return """
                És um stylist pessoal. Com base no clima atual e no armário do utilizador, gera entre 3 a 5 dicas de moda curtas, práticas e diretas para hoje.

                CLIMA ATUAL em %s:
                - Temperatura: %.1f°C (%s)
                - Condição: %s%s%s

                ARMÁRIO DO UTILIZADOR (%d peças):
                %s

                Gera dicas curtas (máx. 10 palavras cada) e diretas. Exemplos do estilo certo:
                - "Está frio — aproveita o casaco de lã"
                - "Com esta chuva, evita as sapatilhas de tecido"
                - "Vento forte — fecha bem a jaqueta"
                - "Hoje é perfeito para roupa leve e respirável"

                Responde APENAS com um JSON array de strings, sem markdown:
                ["dica 1", "dica 2", "dica 3"]
                """.formatted(
                weather.getCity(),
                weather.getTemperature(),
                weather.getTempCategory(),
                weather.getDescription(),
                rainy ? " (CHUVA)" : "",
                windy ? " (VENTO: " + (int) weather.getWindSpeed() + " km/h)" : "",
                wardrobe.size(),
                wardrobeSummary.toString().trim()
        );
    }

    private List<String> parseTips(String json) {
        try {
            String cleaned = json.replaceAll("```json\\s*", "").replaceAll("```\\s*", "").trim();
            JsonNode root = objectMapper.readTree(cleaned);
            if (!root.isArray()) return List.of();
            List<String> tips = new ArrayList<>();
            for (JsonNode node : root) {
                String tip = node.asText("").trim();
                if (!tip.isEmpty()) tips.add(tip);
            }
            return tips.stream().limit(5).toList();
        } catch (Exception e) {
            return List.of();
        }
    }

    private List<String> ruleBased(WeatherData weather) {
        List<String> tips = new ArrayList<>();
        boolean rainy = weather.getWeatherCode() >= 51 && weather.getWeatherCode() <= 82;
        boolean snowy = weather.getWeatherCode() >= 71 && weather.getWeatherCode() <= 77;
        boolean windy = weather.getWindSpeed() > 30;

        switch (weather.getTempCategory()) {
            case "very-cold" -> {
                tips.add("Muito frio — veste várias camadas hoje");
                tips.add("Casaco pesado, cachecol e luvas são essenciais");
            }
            case "cold" -> {
                tips.add("Frio — leva um casaco quente obrigatoriamente");
                tips.add("Uma camada extra por baixo ajuda bastante");
            }
            case "cool" -> {
                tips.add("Fresco — uma jaqueta leve é suficiente");
                tips.add("De noite vai arrefecer, leva algo por cima");
            }
            case "mild" -> {
                tips.add("Temperatura agradável — qualquer look fica bem");
                tips.add("Uma t-shirt com camada opcional é perfeita");
            }
            case "warm" -> {
                tips.add("Quente — opta por roupa leve e respirável");
                tips.add("Lembra-te de protetor solar hoje");
            }
            case "hot" -> {
                tips.add("Muito quente — roupa leve é essencial");
                tips.add("Tecidos naturais como algodão são os melhores");
                tips.add("Hidrata-te bem ao longo do dia");
            }
        }

        if (rainy) {
            tips.add("Leva guarda-chuva — está a chover");
            if (!snowy) tips.add("Calçado impermeável é uma boa ideia hoje");
        }
        if (snowy) tips.add("Neve — botas impermeáveis e roupa quente");
        if (windy) tips.add("Vento forte — evita peças largas e saias");

        return tips.stream().limit(5).toList();
    }

    private String detectWardrobeGap(WeatherData weather, List<WardrobeItem> wardrobe) {
        if (wardrobe.isEmpty()) return null;

        boolean hasJacket = wardrobe.stream().anyMatch(item -> "jackets".equals(item.getCategory()));
        boolean rainy = weather.getWeatherCode() >= 51 && weather.getWeatherCode() <= 82;
        boolean hasWaterproofShoes = wardrobe.stream()
                .anyMatch(item -> "shoes".equals(item.getCategory())
                        && item.getMaterial() != null
                        && (item.getMaterial().contains("leather")
                            || item.getMaterial().contains("synthetic")
                            || item.getMaterial().contains("rubber")));

        String cat = weather.getTempCategory();
        if (("very-cold".equals(cat) || "cold".equals(cat)) && !hasJacket) {
            return "Não tens casaco no armário — ideal para este frio";
        }
        if (rainy && !hasWaterproofShoes) {
            return "Sem calçado impermeável no armário para a chuva";
        }
        return null;
    }
}
