package com.example.demo.controller;

import com.example.demo.domain.WardrobeItem;
import com.example.demo.dto.OutfitSuggestion;
import com.example.demo.dto.OutfitsResponse;
import com.example.demo.dto.WeatherAdvisory;
import com.example.demo.dto.WeatherData;
import com.example.demo.dto.ReserveOutfitRequest;
import java.time.LocalDate;
import com.example.demo.repository.WardrobeItemRepository;
import com.example.demo.service.JwtService;
import com.example.demo.service.OutfitService;
import com.example.demo.service.WeatherAdvisoryService;
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
    private final WeatherAdvisoryService advisoryService;
    private final WardrobeItemRepository wardrobeItemRepository;

    public OutfitController(OutfitService outfitService,
                            JwtService jwtService,
                            WeatherService weatherService,
                            WeatherAdvisoryService advisoryService,
                            WardrobeItemRepository wardrobeItemRepository) {
        this.outfitService = outfitService;
        this.jwtService = jwtService;
        this.weatherService = weatherService;
        this.advisoryService = advisoryService;
        this.wardrobeItemRepository = wardrobeItemRepository;
    }

    @GetMapping
    public ResponseEntity<OutfitsResponse> getOutfits(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lon) {

        Long userId = jwtService.extractUserId(authHeader.replace("Bearer ", ""));

        WeatherData weather = null;
        if (lat != null && lon != null) {
            try {
                weather = weatherService.getWeather(lat, lon);
            } catch (Exception e) {
                System.err.println("Weather fetch failed, proceeding without context: " + e.getMessage());
            }
        }

        List<OutfitSuggestion> outfits = outfitService.generateOutfits(userId, weather);

        WeatherAdvisory advisory = null;
        if (weather != null) {
            try {
                List<WardrobeItem> wardrobe = wardrobeItemRepository.findByUserIdOrderByCreatedAtDesc(userId);
                advisory = advisoryService.generateAdvisory(weather, wardrobe);
            } catch (Exception e) {
                System.err.println("Advisory generation failed: " + e.getMessage());
            }
        }

        return ResponseEntity.ok(new OutfitsResponse(outfits, weather, advisory));
    }

    @GetMapping("/event")
    public ResponseEntity<OutfitsResponse> getOutfitsForEvent(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam String eventName,
            @RequestParam String date,
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lon) {

        Long userId = jwtService.extractUserId(authHeader.replace("Bearer ", ""));
        LocalDate eventDate = LocalDate.parse(date);

        WeatherData weather = null;
        if (lat != null && lon != null) {
            try {
                weather = weatherService.getWeather(lat, lon);
            } catch (Exception e) {
                System.err.println("Weather fetch failed: " + e.getMessage());
            }
        }

        List<OutfitSuggestion> outfits = outfitService.generateOutfitsForEvent(userId, eventName, weather, eventDate);
        return ResponseEntity.ok(new OutfitsResponse(outfits, weather, null));
    }

    @PostMapping("/reserve")
    public ResponseEntity<Void> reserveOutfit(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody ReserveOutfitRequest request) {

        Long userId = jwtService.extractUserId(authHeader.replace("Bearer ", ""));
        outfitService.reserveOutfit(userId, request.getEventName(), request.getEventDate(), request.getItemIds());
        return ResponseEntity.ok().build();
    }
}
