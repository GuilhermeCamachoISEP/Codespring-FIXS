package com.example.demo.controller;

import com.example.demo.dto.InspirationSection;
import com.example.demo.service.InspirationService;
import com.example.demo.service.JwtService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/inspiration")
@CrossOrigin(originPatterns = {"http://localhost:*", "http://127.0.0.1:*"})
public class InspirationController {

    private final InspirationService inspirationService;
    private final JwtService jwtService;

    public InspirationController(InspirationService inspirationService, JwtService jwtService) {
        this.inspirationService = inspirationService;
        this.jwtService = jwtService;
    }

    @GetMapping("/photos")
    public ResponseEntity<Map<String, Object>> getPhotos(
            @RequestHeader("Authorization") String authHeader) {

        if (!inspirationService.isConfigured()) {
            return ResponseEntity.ok(Map.of(
                    "sections", List.of(),
                    "configured", false,
                    "message", "Unsplash API key not configured"
            ));
        }

        Long userId = jwtService.extractUserId(authHeader.replace("Bearer ", ""));
        List<InspirationSection> sections = inspirationService.getInspirationForUser(userId);

        return ResponseEntity.ok(Map.of(
                "sections", sections,
                "configured", true
        ));
    }
}
