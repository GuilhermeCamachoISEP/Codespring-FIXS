package com.example.demo.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "swipe_results")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SwipeResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "image_id", nullable = false)
    private String imageId;

    private boolean liked;
}
