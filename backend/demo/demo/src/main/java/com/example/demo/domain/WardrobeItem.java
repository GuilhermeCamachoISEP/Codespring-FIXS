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

    @Column(name = "image_url", length = 2048)
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

    /** Data em que esta peça foi usada pela última vez num outfit marcado como "usado". */
    @Column(name = "last_used_at")
    private java.time.LocalDate lastUsedAt;

    @PrePersist
    void prePersist() {
        createdAt = LocalDateTime.now();
    }
}
