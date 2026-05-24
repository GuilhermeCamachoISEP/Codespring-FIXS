package com.example.demo.controller;

import com.example.demo.dto.WeatherData;
import com.example.demo.service.WeatherService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/weather")
@CrossOrigin(originPatterns = {"http://localhost:*", "http://127.0.0.1:*"})
public class WeatherController {

    private final WeatherService weatherService;

    public WeatherController(WeatherService weatherService) {
        this.weatherService = weatherService;
    }

    @GetMapping
    public ResponseEntity<WeatherData> getWeather(
            @RequestParam double lat,
            @RequestParam double lon) {
        if (lat < -90 || lat > 90 || lon < -180 || lon > 180) {
            return ResponseEntity.badRequest().build();
        }
        WeatherData data = weatherService.getWeather(lat, lon);
        if (data == null) {
            return ResponseEntity.status(503).build();
        }
        return ResponseEntity.ok(data);
    }
}
