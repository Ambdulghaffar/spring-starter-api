package com.ambdulghaffar.springstarterapi.entity;

import com.ambdulghaffar.springstarterapi.entity.enums.UserEnum;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@EntityListeners(AuditingEntityListener.class)
@Entity
@Builder
@Table(name = "users")
// ⚠️ SOFT DELETE : User n'est JAMAIS vraiment supprimé (UPDATE, pas DELETE).
// ⚠️ NE JAMAIS mettre cascade=REMOVE/ALL ou orphanRemoval=true sur les relations
//    OneToMany de User (postLikes, posts, comments, addresses...).
//    Sinon Hibernate cascade une VRAIE suppression sur les entités liées,
//    alors que User lui-même n'est que soft-deleted → incohérence + bugs
//    (double DELETE, "Unexpected row count", données orphelines supprimées à tort).
@SQLDelete(sql = "UPDATE users SET deleted_at = NOW(), status = 'DELETED', " +
        "email = CONCAT('deleted_', id, '_', email), " +
        "phone_number = CASE WHEN phone_number IS NULL THEN NULL " +
        "ELSE CONCAT('deleted_', id, '_', phone_number) END " +
        "WHERE id = ?")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Email
    @Size(max = 255)
    @Column(nullable = false, unique = true)
    private String email;

    @Size(max = 255)
    @Column(nullable = true)
    private String password;

    @Size(max = 100)
    private String firstName;

    @Size(max = 100)
    private String lastName;

    @Size(max = 50)
    @Column(unique = true)
    private String phoneNumber;

    @ManyToMany
    @JoinTable(name = "user_images",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "media_id"))
    @OrderColumn(name = "position")
    @JsonIgnore
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @Builder.Default
    private List<Media> images = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    private UserEnum.UserRole role = UserEnum.UserRole.USER;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserEnum.UserStatus status = UserEnum.UserStatus.PENDING_VERIFICATION;

    @Builder.Default
    @Column(name = "env_managed", nullable = false)
    private boolean envManaged = false;

    // Soft delete timestamp
    @Column(nullable = true)
    private LocalDateTime deletedAt;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
