package com.ambdulghaffar.springstarterapi.repository;

import com.ambdulghaffar.springstarterapi.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, String> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    @Modifying
    @Query("UPDATE RefreshToken r SET r.revoked = true, r.revokedAt = :now WHERE r.userId = :userId")
    void revokeAllByUserId(@Param("userId") String userId, @Param("now") Instant now);

    @Transactional
    @Modifying
    @Query("DELETE FROM RefreshToken r WHERE r.userId = :userId")
    void deleteByUserId(@Param("userId") String userId);
}