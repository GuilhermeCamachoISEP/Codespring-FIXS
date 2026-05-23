package com.example.demo.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "wardrobe_items")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WardrobeItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "image_url")
    private String imageUrl;

    private String category;
    private String subcategory;
    private String brand;
    private String color;
    private String fit;
    private String material;

    @Column(columnDefinition = "TEXT")
    private String season;       // JSON array: ["fall","winter"]

    @Column(name = "style_tags", columnDefinition = "TEXT")
    private String styleTags;    // JSON array: ["streetwear","casual"]

    @Column(name = "times_used")
    @Builder.Default
    private int timesUsed = 0;

    @Column(name = "favorite_score")
    @Builder.Default
    private double favoriteScore = 0.0;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        createdAt = LocalDateTime.now();
    }
}
