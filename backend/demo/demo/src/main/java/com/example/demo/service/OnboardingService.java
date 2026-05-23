package com.example.demo.service;

import com.example.demo.domain.SwipeResult;
import com.example.demo.domain.UserPreferences;
import com.example.demo.dto.StyleWeightsRequest;
import com.example.demo.dto.SwipeRequest;
import com.example.demo.repository.SwipeResultRepository;
import com.example.demo.repository.UserPreferencesRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OnboardingService {

    private final UserPreferencesRepository preferencesRepository;
    private final SwipeResultRepository swipeResultRepository;
    private final ObjectMapper objectMapper;

    public OnboardingService(UserPreferencesRepository preferencesRepository,
                             SwipeResultRepository swipeResultRepository,
                             ObjectMapper objectMapper) {
        this.preferencesRepository = preferencesRepository;
        this.swipeResultRepository = swipeResultRepository;
        this.objectMapper = objectMapper;
    }

    public UserPreferences saveStyles(Long userId, StyleWeightsRequest req) {
        String json;
        try {
            json = objectMapper.writeValueAsString(req.getStyles());
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize styles", e);
        }
        UserPreferences prefs = preferencesRepository.findByUserId(userId)
                .orElse(UserPreferences.builder().userId(userId).build());
        prefs.setStyleWeights(json);
        prefs.setGender(req.getGender());
        prefs.setAgeRange(req.getAgeRange());
        prefs.setBudgetRange(req.getBudgetRange());
        return preferencesRepository.save(prefs);
    }

    public SwipeResult saveSwipe(Long userId, SwipeRequest req) {
        SwipeResult result = SwipeResult.builder()
                .userId(userId)
                .imageId(req.getImageId())
                .liked(req.isLiked())
                .build();
        return swipeResultRepository.save(result);
    }

    public List<SwipeResult> getSwipes(Long userId) {
        return swipeResultRepository.findByUserId(userId);
    }
}
