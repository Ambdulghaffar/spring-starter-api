package com.ambdulghaffar.springstarterapi.dto.auth;

import com.ambdulghaffar.springstarterapi.dto.ProfileFields;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class AuthDto {
    private AuthDto() {}

    @Schema(name = "LoginRequest")
    public record LoginRequest(
            @NotBlank @Email String email,
            @NotBlank String password
    ) {}

    @Schema(name = "RegisterRequest")
    public record RegisterRequest(
            String firstName, String lastName, String email,
            @Pattern(
                    regexp = ".*\\S.*",
                    message = "Phone number must not be blank")
            @Size(max = 50)String phoneNumber,
            @NotBlank @Size(min = 5, max = 255) String password // TODO: min = 8
    ) implements ProfileFields {}

    @Schema(name = "VerifyEmailRequest")
    public record VerifyEmailRequest(
            @NotBlank String token
    ) {}

    @Schema(name = "ResendVerificationRequest")
    public record ResendVerificationRequest(
            @NotBlank @Email String email
    ) {}

    @Schema(name = "ForgotPasswordRequest")
    public record ForgotPasswordRequest(@NotBlank @Email String email) {}

    @Schema(name = "ResetPasswordRequest")
    public record ResetPasswordRequest(
            @NotBlank String token,
            @NotBlank @Size(min = 8, max = 100) String newPassword
    ) {}

    @Schema(name = "RefreshJwtRequest")
    public record RefreshJwtRequest(
            @NotBlank String refreshToken
    ) {}

    @Schema(name = "SessionAuthResponse")
    public record SessionAuthResponse(
            String sessionId
    ) {}

    @Schema(name = "JwtAuthResponse")
    public record JwtAuthResponse(
            String accessToken,
            String refreshToken
    ) {}
}