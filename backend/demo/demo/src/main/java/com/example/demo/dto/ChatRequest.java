package com.example.demo.dto;

import lombok.Data;
import java.util.List;

@Data
public class ChatRequest {
    private String message;
    private Double lat;
    private Double lon;
    private String mode; // "photo" or "conversational"
    private List<ChatMessage> history;

    @Data
    public static class ChatMessage {
        private String role; // "user" or "model" (Gemini uses "model" instead of "assistant")
        private String content;
    }
}
