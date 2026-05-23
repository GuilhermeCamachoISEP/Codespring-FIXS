package com.example.demo.controller;

import com.example.demo.dto.OutfitSuggestion;
import com.example.demo.dto.WeatherData;
import com.example.demo.service.JwtService;
import com.example.demo.service.OutfitService;
import com.example.demo.service.WeatherService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/outfits")
@CrossOrigin(originPatterns = {"http://localhost:*", "http://127.0.0.1:*"})
public class OutfitController {

    private final OutfitService outfitService;
    private final JwtService jwtService;
    private final WeatherService weatherService;

    public OutfitController(OutfitService outfitService, JwtService jwtService, WeatherService weatherService) {
        this.outfitService = outfitService;
        this.jwtService = jwtService;
        this.weatherService = weatherService;
    }

    @GetMapping
    public ResponseEntity<List<OutfitSuggestion>> getOutfits(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lon) {
        Long userId = jwtService.extractUserId(authHeader.replace("Bearer ", ""));

        WeatherData weather = null;
        if (lat != null && lon != null) {
            try {
                weather = weatherService.getWeather(lat, lon);
            } catch (Exception e) {
                System.err.println("Weather fetch failed, generating outfits without context: " + e.getMessage());
            }
        }

        return ResponseEntity.ok(outfitService.generateOutfits(userId, weather));
    }
}
