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
    @com.fasterxml.jackson.annotation.JsonProperty("isLiked")
    private Boolean isLiked = false;

    /** false = apenas gerado pela IA; true = utilizador confirmou que usou este outfit */
    @Column(nullable = false)
    private boolean worn = false;

    /** Data em que o utilizador marcou o outfit como usado (nullable). */
    @Column(name = "worn_date")
    private java.time.LocalDate wornDate;

    /** Nome do evento se este outfit foi gerado/reservado para um evento (nullable). */
    @Column(name = "event_name")
    private String eventName;

    public OutfitHistory() {
    }

    public OutfitHistory(Long userId, String outfitItems, LocalDateTime wornAt) {
        this.userId = userId;
        this.outfitItems = outfitItems;
        this.wornAt = wornAt;
        this.isLiked = false;
        this.worn = false;
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

    public boolean isWorn() { return worn; }
    public void setWorn(boolean worn) { this.worn = worn; }

    public java.time.LocalDate getWornDate() { return wornDate; }
    public void setWornDate(java.time.LocalDate wornDate) { this.wornDate = wornDate; }

    public String getEventName() { return eventName; }
    public void setEventName(String eventName) { this.eventName = eventName; }
}
