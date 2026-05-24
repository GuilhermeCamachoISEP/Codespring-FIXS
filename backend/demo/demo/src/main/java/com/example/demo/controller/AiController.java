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
        boolean configured = claudeService.isConfigured();
        String testResult = configured ? claudeService.testConnection() : "NOT_CONFIGURED";
        boolean working = "OK".equals(testResult);

        java.util.Map<String, Object> body = new java.util.LinkedHashMap<>();
        body.put("provider", "groq");
        body.put("configured", configured);
        body.put("working", working);
        body.put("model", claudeService.getModel());
        if (!working) body.put("error", testResult);
        return ResponseEntity.ok(body);
    }
}
