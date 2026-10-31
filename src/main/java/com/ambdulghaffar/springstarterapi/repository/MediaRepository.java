package com.ambdulghaffar.springstarterapi.repository;

import com.ambdulghaffar.springstarterapi.entity.Media;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface MediaRepository extends JpaRepository<Media, Long> {

    Optional<Media> findByIdAndOwnerId(Long id, Long ownerId);

    List<Media> findByOwnerIdOrderByCreatedAtDesc(Long ownerId);

    boolean existsByPublicId(String publicId);

    @Query("""
            SELECT m FROM Media m
            WHERE m.owner.id = :ownerId
              AND NOT EXISTS (SELECT 1 FROM User u JOIN u.images i WHERE i = m)
            """)
    List<Media> findUnusedByOwnerId(@Param("ownerId") Long ownerId);

    @Query("""
            SELECT m FROM Media m
            WHERE m.createdAt < :cutoff
              AND NOT EXISTS (SELECT 1 FROM User u JOIN u.images i WHERE i = m)
            """)
    List<Media> findUnusedCreatedBefore(@Param("cutoff") LocalDateTime cutoff);

    @Query("SELECT COUNT(u) > 0 FROM User u JOIN u.images i WHERE i.id = :id")
    boolean isUsedByUser(@Param("id") Long id);

    @Transactional
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    default void detachFromUserImages(Long id) {
        shiftUserImagePositions(id);
        deleteUserImageLinks(id);
    }

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = """
            UPDATE user_images
            SET position = position - 1
            WHERE user_id IN (SELECT user_id FROM user_images WHERE media_id = :id)
              AND position > (SELECT l.position FROM user_images l
                              WHERE l.media_id = :id AND l.user_id = user_images.user_id)
            """, nativeQuery = true)
    void shiftUserImagePositions(@Param("id") Long id);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = "DELETE FROM user_images WHERE media_id = :id", nativeQuery = true)
    void deleteUserImageLinks(@Param("id") Long id);
}