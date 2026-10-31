package com.ambdulghaffar.springstarterapi.dto;

import com.ambdulghaffar.springstarterapi.dto.validation.OnCreate;
import com.ambdulghaffar.springstarterapi.dto.validation.OnUpdate;
import com.ambdulghaffar.springstarterapi.entity.enums.UserEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.UniqueElements;

import java.time.LocalDateTime;
import java.util.List;

public final class UserDto {
    private UserDto() {}

    // =========================================================
    // REQUESTS
    // =========================================================
    // @Notblank means the value must not be blank
    // ❌ exp --> [ null, "", "   " ], are all considered blank
    // @Pattern means the value must match the given regular expression
    // =========================================================

    @Schema(name = "UserRequest")
    public record Request(

            @NotBlank(groups = OnCreate.class)
            @Pattern(groups = OnUpdate.class, regexp = "(?s).*\\S.*", message = "must not be blank")
            @Size(max = 100)
            String firstName,

            @NotBlank(groups = OnCreate.class)
            @Pattern(groups = OnUpdate.class, regexp = "(?s).*\\S.*", message = "must not be blank")
            @Size(max = 100)
            String lastName,

            @NotBlank(groups = OnCreate.class)
            @Pattern(groups = OnUpdate.class, regexp = "(?s).*\\S.*", message = "must not be blank")
            @Email
            @Size(max = 255)
            String email,

            @NotBlank(groups = OnCreate.class)
            @Pattern(groups = OnUpdate.class, regexp = "(?s).*\\S.*", message = "must not be blank")
            @Size(min = 5, max = 255) // TODO: min = 8
            String password,

            @Size(max = 50)
            @Pattern(
                    regexp = ".*\\S.*",
                    message = "Le numéro de téléphone ne doit pas être vide")
            String phoneNumber,

            @Size(max = 10)
            @UniqueElements(message = "Les images ne doivent pas être dupliquées")
            List<Long> imageIds,

            // ❌ Pattern can be used to validate the format of a string.
            UserEnum.UserRole role,
            UserEnum.UserStatus status
    ) {}

    @Schema(name = "UserUpdateProfile")
    public record UpdateProfile(
            @Pattern(regexp = "(?s).*\\S.*", message = "must not be blank")
            @Size(max = 100)
            String firstName,

            @Pattern(regexp = "(?s).*\\S.*", message = "must not be blank")
            @Size(max = 100)
            String lastName,

            @Size(max = 50)
            @Pattern(regexp = ".*\\S.*", message = "Le numéro de téléphone ne doit pas être vide")
            String phoneNumber,

            @Size(max = 10)
            @UniqueElements(message = "Les images ne doivent pas être dupliquées")
            List<Long> imageIds
    ) {}

    public record ImageDto(Long id, String url, int position) {}

    // === RESPONSES ===

    @Schema(name = "UserSummary")
    public record Summary(
            Long id,
            String firstName,
            String lastName,
            String avatarUrl,
            String email,
            List<ImageDto> images
    ) {}

    @Schema(name = "UserResponse")
    public record Response(
            Long id,
            String email,
            String firstName,
            String lastName,
            String phoneNumber,
            String avatarUrl,
            List<ImageDto> images,
            UserEnum.UserRole role,
            UserEnum.UserStatus status,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {}

    // === FILTERS ===

    @Schema(name = "UserFilter")
    public record Filter(
            UserEnum.UserRole role,
            UserEnum.UserStatus status,
            String search
    ) {}
}
