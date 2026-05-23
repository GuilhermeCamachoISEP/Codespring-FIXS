package com.example.demo.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "user_preferences")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserPreferences {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(name = "style_weights", columnDefinition = "TEXT")
    private String styleWeights;

    private String gender;

    @Column(name = "age_range")
    private String ageRange;

    @Column(name = "budget_range")
    private String budgetRange;
}
