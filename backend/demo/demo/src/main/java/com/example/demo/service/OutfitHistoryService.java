package com.example.demo.service;

import com.example.demo.domain.OutfitHistory;
import com.example.demo.domain.WardrobeItem;
import com.example.demo.repository.OutfitHistoryRepository;
import com.example.demo.repository.WardrobeItemRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class OutfitHistoryService {

    private final OutfitHistoryRepository repository;
    private final WardrobeItemRepository wardrobeItemRepository;
    private final ObjectMapper objectMapper;

    public OutfitHistoryService(OutfitHistoryRepository repository,
                                WardrobeItemRepository wardrobeItemRepository,
                                ObjectMapper objectMapper) {
        this.repository = repository;
        this.wardrobeItemRepository = wardrobeItemRepository;
        this.objectMapper = objectMapper;
    }

    /** Saves a newly-generated outfit (worn=false until user confirms). */
    public OutfitHistory saveOutfit(Long userId, String outfitItems) {
        return saveOutfit(userId, outfitItems, null);
    }

    /** Saves an outfit with an optional event name (for event-reserved outfits). */
    public OutfitHistory saveOutfit(Long userId, String outfitItems, String eventName) {
        OutfitHistory history = new OutfitHistory(userId, outfitItems, LocalDateTime.now());
        if (eventName != null && !eventName.isBlank()) {
            history.setEventName(eventName);
        }
        return repository.save(history);
    }

    /**
     * Marks an outfit as actually worn today.
     * Also updates lastUsedAt + timesUsed on each wardrobe item in the outfit.
     */
    public OutfitHistory markAsWorn(Long id, Long userId) {
        OutfitHistory history = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Outfit history not found"));
        if (!history.getUserId().equals(userId)) {
            throw new SecurityException("Not authorized");
        }

        LocalDate today = LocalDate.now();
        history.setWorn(true);
        history.setWornDate(today);
        repository.save(history);

        // Update wardrobe items' lastUsedAt and timesUsed
        try {
            JsonNode items = objectMapper.readTree(history.getOutfitItems());
            if (items.isArray()) {
                for (JsonNode node : items) {
                    long itemId = node.path("id").asLong(-1);
                    if (itemId > 0) {
                        wardrobeItemRepository.findById(itemId).ifPresent(item -> {
                            item.setLastUsedAt(today);
                            item.setTimesUsed(item.getTimesUsed() + 1);
                            wardrobeItemRepository.save(item);
                        });
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[OutfitHistoryService] Failed to update wardrobe items lastUsedAt: " + e.getMessage());
        }

        return history;
    }

    public List<OutfitHistory> getFullHistory(Long userId) {
        return repository.findByUserIdOrderByWornAtDesc(userId);
    }

    /**
     * Returns last 7 outfits the user actually WORE — used for AI anti-repetition.
     * Falls back to last 7 generated if nothing has been marked as worn yet.
     */
    public List<OutfitHistory> getLast7Outfits(Long userId) {
        List<OutfitHistory> worn = repository.findTop7ByUserIdAndWornTrueOrderByWornDateDesc(userId);
        if (!worn.isEmpty()) return worn;
        // Fallback: no worn outfits yet — use generated history so the AI still avoids repeating
        return repository.findTop7ByUserIdOrderByWornAtDesc(userId);
    }

    public List<OutfitHistory> getLikedOutfits(Long userId) {
        return repository.findByUserIdAndIsLikedTrueOrderByWornAtDesc(userId);
    }

    /**
     * Outfits worn in the last N days — used to enforce the "no repeat" window.
     */
    public List<OutfitHistory> getWornSince(Long userId, int days) {
        return repository.findWornSince(userId, LocalDate.now().minusDays(days));
    }

    /** Returns all event-reserved outfits (eventName is set), newest first. */
    public List<OutfitHistory> getEventOutfits(Long userId) {
        return repository.findByUserIdAndEventNameNotNullOrderByWornAtDesc(userId);
    }

    public void toggleLike(Long outfitId, Long userId) {
        OutfitHistory history = repository.findById(outfitId)
                .orElseThrow(() -> new IllegalArgumentException("Outfit history not found"));
        if (!history.getUserId().equals(userId)) {
            throw new SecurityException("Not authorized to modify this outfit history");
        }
        Boolean currentVal = history.getIsLiked();
        history.setIsLiked(currentVal == null || !currentVal);
        repository.save(history);
    }
}
