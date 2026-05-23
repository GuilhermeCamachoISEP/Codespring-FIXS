package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OnboardingStatus {
    private boolean hasStyles;
    private boolean hasWardrobe;
    private boolean complete;
    private long wardrobeCount;
    private String nextStep;
}
