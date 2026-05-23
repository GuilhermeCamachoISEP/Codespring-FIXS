package com.example.demo.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "outfit_history")
public class OutfitHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String outfitItems;

    @Column(nullable = false)
    private LocalDateTime wornAt;

    @Column(nullable = false)
    private Boolean isLiked = false;

    public OutfitHistory() {
    }

    public OutfitHistory(Long userId, String outfitItems, LocalDateTime wornAt) {
        this.userId = userId;
        this.outfitItems = outfitItems;
        this.wornAt = wornAt;
        this.isLiked = false;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getOutfitItems() { return outfitItems; }
    public void setOutfitItems(String outfitItems) { this.outfitItems = outfitItems; }

    public LocalDateTime getWornAt() { return wornAt; }
    public void setWornAt(LocalDateTime wornAt) { this.wornAt = wornAt; }

    public Boolean getIsLiked() { return isLiked; }
    public void setIsLiked(Boolean isLiked) { this.isLiked = isLiked; }
}
