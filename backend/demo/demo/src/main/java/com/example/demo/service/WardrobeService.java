package com.example.demo.service;

import com.example.demo.domain.WardrobeItem;
import com.example.demo.dto.ClothingMetadata;
import com.example.demo.repository.WardrobeItemRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

@Service
public class WardrobeService {

    @Value("${app.upload.dir}")
    private String uploadDir;

    private final WardrobeItemRepository wardrobeItemRepository;
    private final ClaudeService claudeService;
    private final ObjectMapper objectMapper;

    public WardrobeService(WardrobeItemRepository wardrobeItemRepository,
                           ClaudeService claudeService,
                           ObjectMapper objectMapper) {
        this.wardrobeItemRepository = wardrobeItemRepository;
        this.claudeService = claudeService;
        this.objectMapper = objectMapper;
    }

    public WardrobeItem uploadAndClassify(Long userId, MultipartFile file) throws IOException {
        ensureUploadDirExists();

        String originalName = file.getOriginalFilename();
        String extension = (originalName != null && originalName.contains("."))
                ? originalName.substring(originalName.lastIndexOf('.'))
                : ".jpg";
        String filename = UUID.randomUUID() + extension;

        Path destination = Paths.get(uploadDir).resolve(filename).toAbsolutePath();
        Files.copy(file.getInputStream(), destination);

        String mediaType = resolveMediaType(extension);
        byte[] imageBytes = file.getBytes();
        ClothingMetadata meta = claudeService.classifyClothing(imageBytes, mediaType);

        WardrobeItem item = WardrobeItem.builder()
                .userId(userId)
                .imageUrl("/uploads/" + filename)
                .category(meta.getCategory())
                .subcategory(meta.getSubcategory())
                .color(meta.getColor())
                .fit(meta.getFit())
                .material(meta.getMaterial())
                .brand(meta.getBrand() != null ? meta.getBrand() : "unknown")
                .season(toJson(meta.getSeason()))
                .styleTags(toJson(meta.getStyle_tags()))
                .build();

        return wardrobeItemRepository.save(item);
    }

    public List<WardrobeItem> getWardrobe(Long userId) {
        return wardrobeItemRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public List<WardrobeItem> getWardrobeByCategory(Long userId, String category) {
        return wardrobeItemRepository.findByUserIdAndCategory(userId, category);
    }

    public void deleteItem(Long userId, Long itemId) {
        WardrobeItem item = wardrobeItemRepository.findById(itemId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!item.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        wardrobeItemRepository.delete(item);
    }

    public long countByUser(Long userId) {
        return wardrobeItemRepository.countByUserId(userId);
    }

    private void ensureUploadDirExists() throws IOException {
        Path dir = Paths.get(uploadDir).toAbsolutePath();
        if (!Files.exists(dir)) {
            Files.createDirectories(dir);
        }
    }

    private String toJson(List<String> list) {
        try {
            return objectMapper.writeValueAsString(list != null ? list : List.of());
        } catch (Exception e) {
            return "[]";
        }
    }

    private String resolveMediaType(String ext) {
        return switch (ext.toLowerCase()) {
            case ".png" -> "image/png";
            case ".gif" -> "image/gif";
            case ".webp" -> "image/webp";
            default -> "image/jpeg";
        };
    }
}
