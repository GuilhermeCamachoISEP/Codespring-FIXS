package com.example.demo.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "app_users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;

    private String name;

    @Column(name = "gender")
    private String gender;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    // Pinterest OAuth — dormant (no UI), kept so existing DB rows with NULL don't crash
    @Column(name = "pinterest_access_token", length = 2048)
    private String pinterestAccessToken;

    @Column(name = "pinterest_refresh_token", length = 2048)
    private String pinterestRefreshToken;

    // Boolean wrapper (not primitive) so legacy NULL rows are handled safely
    @Column(name = "pinterest_connected")
    @Builder.Default
    private Boolean pinterestConnected = false;

    @PrePersist
    void prePersist() {
        createdAt = LocalDateTime.now();
        if (pinterestConnected == null) pinterestConnected = false;
    }
}
