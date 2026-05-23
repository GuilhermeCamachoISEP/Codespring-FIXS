package com.example.demo.controller;

import com.example.demo.service.ClaudeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/ai")
@CrossOrigin(originPatterns = {"http://localhost:*", "http://127.0.0.1:*"})
public class AiController {

    private final ClaudeService claudeService;

    public AiController(ClaudeService claudeService) {
        this.claudeService = claudeService;
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> status() {
        return ResponseEntity.ok(Map.of(
                "provider", "groq",
                "configured", claudeService.isConfigured(),
                "model", claudeService.getModel()
        ));
    }
}
