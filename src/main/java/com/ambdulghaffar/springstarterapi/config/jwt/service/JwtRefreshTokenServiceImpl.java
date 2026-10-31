package com.ambdulghaffar.springstarterapi.config.jwt.service;

import com.ambdulghaffar.springstarterapi.entity.RefreshToken;
import com.ambdulghaffar.springstarterapi.properties.JwtProperties;
import com.ambdulghaffar.springstarterapi.repository.RefreshTokenRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

@Slf4j
@Service
@Profile("jwt")
@RequiredArgsConstructor
public class JwtRefreshTokenServiceImpl implements JwtRefreshTokenService {
    private final RefreshTokenRepository repository;
    private final JwtProperties properties;

    // Fenêtre de tolérance pour absorber les race conditions légitimes
    // (plusieurs requêtes front en parallèle qui refresh en même temps).
    // Au-delà de cette fenêtre, une réutilisation est considérée comme un vol.
    private Duration reuseGracePeriod() {
        return Duration.ofMillis(properties.refreshTokenReuseGracePeriod());
    }

    @Override
    public String issue(String userId, HttpServletRequest request) {
        String rawToken = UUID.randomUUID().toString() + UUID.randomUUID();
        String hash = sha256(rawToken);

        RefreshToken entity = RefreshToken.builder()
                .tokenHash(hash)
                .userId(userId)
                .expiresAt(Instant.now().plusMillis(properties.refreshTokenExpiration()))
                .revoked(false)
                .createdAt(Instant.now())
                .userAgent(request.getHeader("User-Agent"))
                .ipAddress(request.getRemoteAddr())
                .build();

        repository.save(entity);
        return rawToken;
    }

    @Override
    @Transactional(noRollbackFor = ResponseStatusException.class)
    public String rotate(String rawToken, HttpServletRequest request) {
        String hash = sha256(rawToken);
        RefreshToken existing = repository.findByTokenHash(hash)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token inconnu"));

        // 1. Expiré : simple refus, ce n'est pas un signal de vol.
        if (existing.getExpiresAt().isBefore(Instant.now())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token expiré");
        }

        // 2. Déjà tourné :  grace period ou vol.
        if (existing.isRevoked()) {
            boolean withinGracePeriod = existing.getRevokedAt() != null
                    && existing.getRevokedAt().isAfter(Instant.now().minus(reuseGracePeriod()));

            if (withinGracePeriod) {
                log.debug("Refresh token reuse within grace period for user {}", existing.getUserId());
                return existing.getUserId();
            }

            log.warn("Refresh token reuse detected outside grace period for user {} — revoking all sessions", existing.getUserId());
            repository.revokeAllByUserId(existing.getUserId(), Instant.now());
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "Token déjà utilisé — toutes les sessions ont été révoquées");
        }

        // 3. Rotation normale.
        existing.setRevoked(true);
        existing.setRevokedAt(Instant.now());
        repository.save(existing);

        return existing.getUserId();
    }

    @Override
    @Transactional
    public void revokeAllForUser(String userId) {
        repository.revokeAllByUserId(userId, Instant.now());
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes());
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}