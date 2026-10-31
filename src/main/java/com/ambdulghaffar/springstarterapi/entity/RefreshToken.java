package com.ambdulghaffar.springstarterapi.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "refresh_tokens")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false, unique = true)
    private String tokenHash;

    @Column(nullable = false)
    private String userId;

    @Column(nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private boolean revoked;

    private Instant revokedAt;

    @Column(nullable = false)
    private Instant createdAt;

    /**
     * Utile pour tracker/afficher les sessions actives à l'utilisateur.
     */
    private String userAgent;
    private String ipAddress;
}