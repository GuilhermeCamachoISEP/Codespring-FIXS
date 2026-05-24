package com.example.demo.controller;

import com.example.demo.dto.PackingRequest;
import com.example.demo.dto.PackingResponse;
import com.example.demo.service.JwtService;
import com.example.demo.service.PackingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/packing")
@CrossOrigin(originPatterns = {"http://localhost:*", "http://127.0.0.1:*"})
public class PackingController {

    private final PackingService packingService;
    private final JwtService jwtService;

    public PackingController(PackingService packingService, JwtService jwtService) {
        this.packingService = packingService;
        this.jwtService = jwtService;
    }

    @PostMapping
    public ResponseEntity<?> planPacking(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody PackingRequest request) {
        
        try {
            Long userId = jwtService.extractUserId(authHeader.replace("Bearer ", ""));
            PackingResponse response = packingService.generatePackingList(userId, request.getTripDescription(), request.getTravelDate());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Erro ao planear mala: " + e.getMessage());
        }
    }
}
