package com.example.demo.controller;

import com.example.demo.domain.SwipeResult;
import com.example.demo.domain.UserPreferences;
import com.example.demo.dto.StyleWeightsRequest;
import com.example.demo.dto.SwipeRequest;
import com.example.demo.service.JwtService;
import com.example.demo.service.OnboardingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/onboarding")
@CrossOrigin(origins = "http://localhost:5173")
public class OnboardingController {

    private final OnboardingService onboardingService;
    private final JwtService jwtService;

    public OnboardingController(OnboardingService onboardingService, JwtService jwtService) {
        this.onboardingService = onboardingService;
        this.jwtService = jwtService;
    }

    @PostMapping("/styles")
    public ResponseEntity<UserPreferences> saveStyles(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody StyleWeightsRequest req) {
        return ResponseEntity.ok(onboardingService.saveStyles(extractUserId(authHeader), req));
    }

    @PostMapping("/swipe")
    public ResponseEntity<SwipeResult> swipe(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody SwipeRequest req) {
        return ResponseEntity.ok(onboardingService.saveSwipe(extractUserId(authHeader), req));
    }

    @GetMapping("/swipes")
    public ResponseEntity<List<SwipeResult>> getSwipes(
            @RequestHeader("Authorization") String authHeader) {
        return ResponseEntity.ok(onboardingService.getSwipes(extractUserId(authHeader)));
    }

    private Long extractUserId(String authHeader) {
        return jwtService.extractUserId(authHeader.replace("Bearer ", ""));
    }
}
