package com.ambdulghaffar.springstarterapi.entity;

import com.ambdulghaffar.springstarterapi.entity.enums.UserEnum;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "email_tokens")
@Data
@NoArgsConstructor
public class EmailToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String tokenHash;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private UserEnum.EmailTokenType type;

    @Column(nullable = false)
    private Instant expiresAt;

    private Instant usedAt;

    public EmailToken(String tokenHash, User user, UserEnum.EmailTokenType type, Instant expiresAt) {
        this.tokenHash = tokenHash;
        this.user = user;
        this.type = type;
        this.expiresAt = expiresAt;
    }

    public boolean isUsable() {
        return usedAt == null && expiresAt.isAfter(Instant.now());
    }
}