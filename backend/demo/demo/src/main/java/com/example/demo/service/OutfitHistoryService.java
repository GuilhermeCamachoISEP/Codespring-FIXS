package com.example.demo.service;

import com.example.demo.domain.OutfitHistory;
import com.example.demo.repository.OutfitHistoryRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class OutfitHistoryService {

    private final OutfitHistoryRepository repository;

    public OutfitHistoryService(OutfitHistoryRepository repository) {
        this.repository = repository;
    }

    public OutfitHistory saveOutfit(Long userId, String outfitItems) {
        OutfitHistory history = new OutfitHistory(userId, outfitItems, LocalDateTime.now());
        return repository.save(history);
    }

    public List<OutfitHistory> getFullHistory(Long userId) {
        return repository.findByUserIdOrderByWornAtDesc(userId);
    }

    public List<OutfitHistory> getLast7Outfits(Long userId) {
        return repository.findTop7ByUserIdOrderByWornAtDesc(userId);
    }

    public List<OutfitHistory> getLikedOutfits(Long userId) {
        return repository.findByUserIdAndIsLikedTrueOrderByWornAtDesc(userId);
    }

    public void toggleLike(Long outfitId, Long userId) {
        OutfitHistory history = repository.findById(outfitId)
                .orElseThrow(() -> new IllegalArgumentException("Outfit history not found"));
        
        if (!history.getUserId().equals(userId)) {
            throw new SecurityException("Not authorized to modify this outfit history");
        }
        
        history.setIsLiked(!history.getIsLiked());
        repository.save(history);
    }
}
