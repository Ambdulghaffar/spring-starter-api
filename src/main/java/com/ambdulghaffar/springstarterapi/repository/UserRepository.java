package com.ambdulghaffar.springstarterapi.repository;

import com.ambdulghaffar.springstarterapi.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {
    @Query("SELECT u FROM User u WHERE u.email = :email")
    Optional<User> findByEmail(String email);

    @Modifying
    @Query(value = "DELETE FROM users WHERE id = :id", nativeQuery = true)
    void hardDelete(@Param("id") Long id);

    boolean existsByEmail(String email);

    Optional<User> findFirstByEnvManagedTrueAndDeletedAtIsNull();
}

