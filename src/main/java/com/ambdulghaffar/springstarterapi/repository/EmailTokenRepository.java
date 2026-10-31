package com.ambdulghaffar.springstarterapi.repository;

import com.ambdulghaffar.springstarterapi.entity.EmailToken;
import com.ambdulghaffar.springstarterapi.entity.User;
import com.ambdulghaffar.springstarterapi.entity.enums.UserEnum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface EmailTokenRepository extends JpaRepository<EmailToken, Long> {

    Optional<EmailToken> findByTokenHashAndType(String tokenHash, UserEnum.EmailTokenType type);

    @Modifying
    @Query("delete from EmailToken t where t.user = :user and t.type = :type")
    void deleteByUserAndType(@Param("user") User user, @Param("type") UserEnum.EmailTokenType type);

    @Modifying
    @Query("delete from EmailToken t where t.expiresAt < :now")
    int deleteExpired(@Param("now") Instant now);

    @Modifying
    @Query("DELETE FROM EmailToken t WHERE t.user.id = :userId")
    void deleteByUser_Id(@Param("userId") Long userId);
}