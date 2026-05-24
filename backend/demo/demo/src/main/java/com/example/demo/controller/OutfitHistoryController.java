package com.example.demo.controller;

import com.example.demo.domain.OutfitHistory;
import com.example.demo.service.JwtService;
import com.example.demo.service.OutfitHistoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/outfits/history")
@CrossOrigin(originPatterns = {"http://localhost:*", "http://127.0.0.1:*"})
public class OutfitHistoryController {

    private final OutfitHistoryService historyService;
    private final JwtService jwtService;

    public OutfitHistoryController(OutfitHistoryService historyService, JwtService jwtService) {
        this.historyService = historyService;
        this.jwtService = jwtService;
    }

    @PostMapping
    public ResponseEntity<OutfitHistory> saveOutfit(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Map<String, Object> payload) {
        Long userId = jwtService.extractUserId(authHeader.replace("Bearer ", ""));

        try {
            String outfitItemsJson = new com.fasterxml.jackson.databind.ObjectMapper()
                    .writeValueAsString(payload.get("outfitItems"));
            // Optional event name — present when reserving for a calendar event
            String eventName = payload.containsKey("eventName")
                    ? String.valueOf(payload.get("eventName"))
                    : null;
            if ("null".equals(eventName) || "".equals(eventName)) eventName = null;
            OutfitHistory saved = historyService.saveOutfit(userId, outfitItemsJson, eventName);
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping
    public ResponseEntity<List<OutfitHistory>> getHistory(@RequestHeader("Authorization") String authHeader) {
        Long userId = jwtService.extractUserId(authHeader.replace("Bearer ", ""));
        return ResponseEntity.ok(historyService.getFullHistory(userId));
    }

    @GetMapping("/liked")
    public ResponseEntity<List<OutfitHistory>> getLikedHistory(@RequestHeader("Authorization") String authHeader) {
        Long userId = jwtService.extractUserId(authHeader.replace("Bearer ", ""));
        return ResponseEntity.ok(historyService.getLikedOutfits(userId));
    }

    @PatchMapping("/{id}/like")
    public ResponseEntity<Void> toggleLike(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long id) {
        Long userId = jwtService.extractUserId(authHeader.replace("Bearer ", ""));
        historyService.toggleLike(id, userId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/reserved")
    public ResponseEntity<List<OutfitHistory>> getEventOutfits(
            @RequestHeader("Authorization") String authHeader) {
        Long userId = jwtService.extractUserId(authHeader.replace("Bearer ", ""));
        return ResponseEntity.ok(historyService.getEventOutfits(userId));
    }

    @PatchMapping("/{id}/worn")
    public ResponseEntity<OutfitHistory> markAsWorn(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long id) {
        Long userId = jwtService.extractUserId(authHeader.replace("Bearer ", ""));
        OutfitHistory updated = historyService.markAsWorn(id, userId);
        return ResponseEntity.ok(updated);
    }
}
