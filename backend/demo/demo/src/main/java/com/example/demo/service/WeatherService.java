package com.example.demo.service;

import com.example.demo.dto.WeatherData;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Service
public class WeatherService {

    private static final String OPEN_METEO_URL =
            "https://api.open-meteo.com/v1/forecast?latitude=%s&longitude=%s&current_weather=true";

    private static final String NOMINATIM_URL =
            "https://nominatim.openstreetmap.org/reverse?lat=%s&lon=%s&format=json&accept-language=pt";

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();
    private final ObjectMapper objectMapper;

    public WeatherService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public WeatherData getWeather(double lat, double lon) {
        try {
            HttpRequest weatherRequest = HttpRequest.newBuilder()
                    .uri(URI.create(OPEN_METEO_URL.formatted(lat, lon)))
                    .header("Accept", "application/json")
                    .GET()
                    .build();
            HttpResponse<String> weatherResponse = httpClient.send(weatherRequest, HttpResponse.BodyHandlers.ofString());
            if (weatherResponse.statusCode() != 200) {
                System.err.println("Open-Meteo error " + weatherResponse.statusCode() + ": " + weatherResponse.body());
                return null;
            }

            JsonNode root = objectMapper.readTree(weatherResponse.body());
            JsonNode current = root.path("current_weather");
            if (current.isMissingNode()) {
                // fallback for older response shape
                current = root.path("current");
            }
            if (current.isMissingNode()) {
                System.err.println("Open-Meteo response missing current_weather: " + weatherResponse.body());
                return null;
            }

            double temperature = current.has("temperature")
                    ? current.path("temperature").asDouble()
                    : current.path("temperature_2m").asDouble();
            int weatherCode = current.has("weathercode")
                    ? current.path("weathercode").asInt()
                    : current.path("weather_code").asInt();
            double windSpeed = current.has("windspeed")
                    ? current.path("windspeed").asDouble()
                    : current.path("wind_speed_10m").asDouble();

            String city = fetchCity(lat, lon);

            return new WeatherData(
                    temperature,
                    describeWeather(weatherCode),
                    city,
                    weatherCode,
                    windSpeed,
                    categorizeTemp(temperature),
                    weatherIcon(weatherCode)
            );
        } catch (Exception e) {
            System.err.println("Weather API failed (" + e.getMessage() + "), returning null weather.");
            return null;
        }
    }

    private String fetchCity(double lat, double lon) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(NOMINATIM_URL.formatted(lat, lon)))
                    .header("User-Agent", "Codespring-FIXS/1.0 (fashion-app)")
                    .header("Accept", "application/json")
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode address = objectMapper.readTree(response.body()).path("address");

            for (String field : new String[]{"city", "town", "village", "municipality", "county"}) {
                JsonNode node = address.path(field);
                if (!node.isMissingNode() && !node.asText().isBlank()) {
                    return node.asText();
                }
            }
            return "A tua localização";
        } catch (Exception e) {
            return "A tua localização";
        }
    }

    private String describeWeather(int code) {
        return switch (code) {
            case 0 -> "Céu limpo";
            case 1 -> "Principalmente limpo";
            case 2 -> "Parcialmente nublado";
            case 3 -> "Nublado";
            case 45, 48 -> "Nevoeiro";
            case 51, 53, 55 -> "Chuvisco";
            case 61, 63, 65 -> "Chuva";
            case 71, 73, 75 -> "Neve";
            case 80, 81, 82 -> "Aguaceiros";
            case 95, 96, 99 -> "Trovoada";
            default -> "Variável";
        };
    }

    private String categorizeTemp(double temp) {
        if (temp < 5) return "very-cold";
        if (temp < 12) return "cold";
        if (temp < 18) return "cool";
        if (temp < 24) return "mild";
        if (temp < 30) return "warm";
        return "hot";
    }

    private String weatherIcon(int code) {
        if (code == 0) return "☀️";
        if (code <= 2) return "🌤️";
        if (code == 3) return "☁️";
        if (code <= 48) return "🌫️";
        if (code <= 67) return "🌧️";
        if (code <= 77) return "❄️";
        if (code <= 82) return "🌦️";
        return "⛈️";
    }
}
