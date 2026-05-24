package com.example.demo.dto;

import lombok.Data;

import java.util.List;

@Data
public class ChatRequest {
    private String message;
    private Double lat;
    private Double lon;
    private String mode; // "conversational", "outfit-refine", etc.
    private List<ChatMessage> history;
    private ChatContext context;

    @Data
    public static class ChatMessage {
        private String role; // "user" or "model"
        private String content;
    }

    @Data
    public static class ChatContext {
        private OutfitSuggestion currentOutfit;
        private List<com.example.demo.domain.WardrobeItem> wardrobeItems;
    }
}
