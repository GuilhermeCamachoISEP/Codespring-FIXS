package com.example.demo.controller;

import com.example.demo.domain.User;
import com.example.demo.dto.ChatRequest;
import com.example.demo.dto.ChatResponse;
import com.example.demo.service.ChatService;
import org.springframework.web.bind.annotation.*;
import com.example.demo.service.JwtService;
import org.springframework.http.ResponseEntity;

@RestController
@CrossOrigin(origins = "*")
public class ChatController {

    private final ChatService chatService;
    private final JwtService jwtService;

    public ChatController(ChatService chatService, JwtService jwtService) {
        this.chatService = chatService;
        this.jwtService = jwtService;
    }

    @PostMapping("/chat")
    public ResponseEntity<ChatResponse> chat(@RequestHeader("Authorization") String authHeader, @RequestBody ChatRequest request) {
        Long userId = jwtService.extractUserId(authHeader.replace("Bearer ", ""));
        ChatResponse response = chatService.processChat(userId, request);
        return ResponseEntity.ok(response);
    }
}
