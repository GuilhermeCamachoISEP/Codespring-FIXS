package com.example.demo.service;

import com.example.demo.domain.SwipeResult;
import com.example.demo.domain.UserPreferences;
import com.example.demo.dto.OnboardingStatus;
import com.example.demo.dto.StyleWeightsRequest;
import com.example.demo.dto.SwipeRequest;
import com.example.demo.repository.SwipeResultRepository;
import com.example.demo.repository.UserPreferencesRepository;
import com.example.demo.repository.WardrobeItemRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OnboardingService {

    private final UserPreferencesRepository preferencesRepository;
    private final SwipeResultRepository swipeResultRepository;
    private final WardrobeItemRepository wardrobeItemRepository;
    private final ObjectMapper objectMapper;

    public OnboardingService(UserPreferencesRepository preferencesRepository,
                             SwipeResultRepository swipeResultRepository,
                             WardrobeItemRepository wardrobeItemRepository,
                             ObjectMapper objectMapper) {
        this.preferencesRepository = preferencesRepository;
        this.swipeResultRepository = swipeResultRepository;
        this.wardrobeItemRepository = wardrobeItemRepository;
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

    public UserPreferences getStyles(Long userId) {
        return preferencesRepository.findByUserId(userId).orElse(null);
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

    public OnboardingStatus getStatus(Long userId) {
        boolean hasStyles = preferencesRepository.findByUserId(userId)
                .map(UserPreferences::getStyleWeights)
                .map(styles -> !styles.isBlank() && !"{}".equals(styles))
                .orElse(false);
        long wardrobeCount = wardrobeItemRepository.countByUserId(userId);
        boolean hasWardrobe = wardrobeCount > 0;
        boolean complete = hasStyles && hasWardrobe;
        String nextStep = !hasStyles ? "/onboarding/styles" : !hasWardrobe ? "/onboarding/wardrobe" : "/dashboard";
        return new OnboardingStatus(hasStyles, hasWardrobe, complete, wardrobeCount, nextStep);
    }
}
