package com.example.demo.service;

import com.example.demo.domain.OutfitReservation;
import com.example.demo.domain.WardrobeItem;
import com.example.demo.dto.ClothingMetadata;
import com.example.demo.repository.OutfitReservationRepository;
import com.example.demo.repository.WardrobeItemRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
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
    private final OutfitReservationRepository outfitReservationRepository;
    private final ClaudeService claudeService;
    private final ObjectMapper objectMapper;

    public WardrobeService(WardrobeItemRepository wardrobeItemRepository,
                           OutfitReservationRepository outfitReservationRepository,
                           ClaudeService claudeService,
                           ObjectMapper objectMapper) {
        this.wardrobeItemRepository = wardrobeItemRepository;
        this.outfitReservationRepository = outfitReservationRepository;
        this.claudeService = claudeService;
        this.objectMapper = objectMapper;
    }

    public WardrobeItem uploadAndClassify(Long userId, MultipartFile file, String categoryHint) throws IOException {
        ensureUploadDirExists();

        String originalName = file.getOriginalFilename();
        String extension = (originalName != null && originalName.contains("."))
                ? originalName.substring(originalName.lastIndexOf('.'))
                : ".jpg";
        String filename = UUID.randomUUID() + extension;

        Path destination = Paths.get(uploadDir).resolve(filename).toAbsolutePath();
        Files.copy(file.getInputStream(), destination);

        byte[] imageBytes = file.getBytes();
        // Resize before AI call — Groq vision has a 4 MB base64 limit (~3 MB raw)
        byte[] aiBytes = resizeForAI(imageBytes);
        String mediaType = aiBytes == imageBytes ? resolveMediaType(extension) : "image/jpeg";
        ClothingMetadata meta = claudeService.classifyClothing(aiBytes, mediaType, categoryHint);

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

    public void deleteItem(Long userId, Long itemId, boolean force) {
        WardrobeItem item = wardrobeItemRepository.findById(itemId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!item.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        List<OutfitReservation> reservations = outfitReservationRepository.findByWardrobeItemId(itemId);
        if (!reservations.isEmpty() && !force) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Item is reserved for an outfit");
        }

        outfitReservationRepository.deleteByWardrobeItemId(itemId);
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

    /**
     * Scales an image down to at most 1024×1024 px and re-encodes as JPEG so the
     * base64 payload sent to Groq Vision stays within its 4 MB limit.
     * Returns the original bytes untouched if already small enough.
     */
    private byte[] resizeForAI(byte[] original) {
        // ~2.5 MB raw → ~3.3 MB base64, safely under the 4 MB Groq limit
        final int MAX_DIM = 1024;
        final int MAX_BYTES = 2_500_000;

        try {
            if (original.length <= MAX_BYTES) {
                BufferedImage probe = ImageIO.read(new ByteArrayInputStream(original));
                if (probe == null || (probe.getWidth() <= MAX_DIM && probe.getHeight() <= MAX_DIM)) {
                    return original; // already fine
                }
            }

            BufferedImage img = ImageIO.read(new ByteArrayInputStream(original));
            if (img == null) return original;

            int w = img.getWidth(), h = img.getHeight();
            double scale = Math.min((double) MAX_DIM / w, (double) MAX_DIM / h);
            int newW = Math.max(1, (int) (w * scale));
            int newH = Math.max(1, (int) (h * scale));

            BufferedImage resized = new BufferedImage(newW, newH, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2d = resized.createGraphics();
            g2d.drawImage(img, 0, 0, newW, newH, null);
            g2d.dispose();

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(resized, "jpeg", out);
            System.out.println("[WardrobeService] Resized image for AI: "
                    + original.length / 1024 + " KB → " + out.size() / 1024 + " KB");
            return out.toByteArray();

        } catch (Exception e) {
            System.err.println("[WardrobeService] Image resize failed, using original: " + e.getMessage());
            return original;
        }
    }
}
