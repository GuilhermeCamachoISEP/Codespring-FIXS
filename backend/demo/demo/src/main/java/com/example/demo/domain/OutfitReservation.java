package com.example.demo.domain;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "OUTFIT_RESERVATIONS")
public class OutfitReservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wardrobe_item_id", nullable = false)
    private WardrobeItem wardrobeItem;

    @Column(nullable = false)
    private LocalDate eventDate;

    @Column(nullable = false)
    private String eventName;

    public OutfitReservation() {
    }

    public OutfitReservation(Long userId, WardrobeItem wardrobeItem, LocalDate eventDate, String eventName) {
        this.userId = userId;
        this.wardrobeItem = wardrobeItem;
        this.eventDate = eventDate;
        this.eventName = eventName;
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public WardrobeItem getWardrobeItem() { return wardrobeItem; }
    public void setWardrobeItem(WardrobeItem wardrobeItem) { this.wardrobeItem = wardrobeItem; }
    public LocalDate getEventDate() { return eventDate; }
    public void setEventDate(LocalDate eventDate) { this.eventDate = eventDate; }
    public String getEventName() { return eventName; }
    public void setEventName(String eventName) { this.eventName = eventName; }
}
