package com.example.demo.controller;

import com.example.demo.domain.WardrobeItem;
import com.example.demo.service.JwtService;
import com.example.demo.service.WardrobeService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin(origins = "http://localhost:5173")
public class WardrobeController {

    @Value("${app.upload.dir}")
    private String uploadDir;

    private final WardrobeService wardrobeService;
    private final JwtService jwtService;

    public WardrobeController(WardrobeService wardrobeService, JwtService jwtService) {
        this.wardrobeService = wardrobeService;
        this.jwtService = jwtService;
    }

    // Upload + Claude Vision classification
    @PostMapping(value = "/wardrobe/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<WardrobeItem> upload(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam("file") MultipartFile file) throws IOException {
        Long userId = extractUserId(authHeader);
        WardrobeItem item = wardrobeService.uploadAndClassify(userId, file);
        return ResponseEntity.ok(item);
    }

    // Get all wardrobe items
    @GetMapping("/wardrobe")
    public ResponseEntity<List<WardrobeItem>> getWardrobe(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam(value = "category", required = false) String category) {
        Long userId = extractUserId(authHeader);
        List<WardrobeItem> items = category != null
                ? wardrobeService.getWardrobeByCategory(userId, category)
                : wardrobeService.getWardrobe(userId);
        return ResponseEntity.ok(items);
    }

    // Count items
    @GetMapping("/wardrobe/count")
    public ResponseEntity<Map<String, Long>> count(
            @RequestHeader("Authorization") String authHeader) {
        Long userId = extractUserId(authHeader);
        return ResponseEntity.ok(Map.of("count", wardrobeService.countByUser(userId)));
    }

    // Delete item
    @DeleteMapping("/wardrobe/{id}")
    public ResponseEntity<Void> delete(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long id) {
        wardrobeService.deleteItem(extractUserId(authHeader), id);
        return ResponseEntity.noContent().build();
    }

    // Serve uploaded images
    @GetMapping("/uploads/{filename}")
    public ResponseEntity<Resource> serveFile(@PathVariable String filename) {
        var file = Paths.get(uploadDir).toAbsolutePath().resolve(filename);
        Resource resource = new FileSystemResource(file);
        if (!resource.exists()) return ResponseEntity.notFound().build();
        String ext = filename.toLowerCase();
        MediaType mediaType = ext.endsWith(".png") ? MediaType.IMAGE_PNG : MediaType.IMAGE_JPEG;
        return ResponseEntity.ok().contentType(mediaType).body(resource);
    }

    private Long extractUserId(String authHeader) {
        return jwtService.extractUserId(authHeader.replace("Bearer ", ""));
    }
}
