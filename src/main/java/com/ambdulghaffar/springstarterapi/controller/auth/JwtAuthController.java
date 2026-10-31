package com.ambdulghaffar.springstarterapi.controller.auth;

import com.ambdulghaffar.springstarterapi.dto.UserDto;
import com.ambdulghaffar.springstarterapi.dto.auth.AuthDto;
import com.ambdulghaffar.springstarterapi.services.auth.AuthService;
import com.ambdulghaffar.springstarterapi.services.auth.jwt.JwtAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@Profile("jwt")
@RequiredArgsConstructor
@Tag(name = "JWT Auth", description = "Login and registration")
public class JwtAuthController {
    private final AuthService authService;
    private final JwtAuthService jwtAuthService;

    @PostMapping("/register")
    @Operation(
            summary = "Create a new account",
            description = "Registers a new user with the USER role and INACTIVE status."
    )
    public ResponseEntity<UserDto.Response> register(
            @Valid @RequestBody AuthDto.RegisterRequest request
    ) {
        UserDto.Response response = authService.register(request);
        log.info("POST /api/v1/auth/register - Account created with id: {}", response.id());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/verify-email")
    @Operation(
            summary = "Confirm an email address",
            description = "Activates the account using the token received by email."
    )
    public ResponseEntity<Map<String, String>> verifyEmail(
            @Valid @RequestBody AuthDto.VerifyEmailRequest request) {
        authService.verifyEmail(request);
        log.info("POST /api/v1/auth/verify-email - Email verification processed");
        return ResponseEntity.ok(Map.of("message", "Email confirmé avec succès."));
    }

    @PostMapping("/resend-verification")
    @Operation(
            summary = "Resend the confirmation email",
            description = "Sends a new confirmation link if a pending account exists for this email. " +
                    "Does not confirm whether the email is registered, for security reasons."
    )
    public ResponseEntity<Map<String, String>> resendVerification(
            @Valid @RequestBody AuthDto.ResendVerificationRequest request) {
        authService.resendVerification(request);
        log.info("POST /api/v1/auth/resend-verification - Resend requested");
        return ResponseEntity.ok(Map.of("message",
                "Si un compte en attente existe avec cet email, un nouveau lien a été envoyé."));
    }

    @PostMapping("/login")
    @Operation(
            summary = "Log in a user",
            description = "Authenticates a user with email and password and returns JWT tokens."
    )
    public AuthDto.JwtAuthResponse login(@RequestBody AuthDto.LoginRequest request, HttpServletRequest httpRequest) {
        log.info("POST /api/v1/auth/login - Authenticating user with email: {}", request.email());
        return jwtAuthService.login(request, httpRequest);
    }

    @PostMapping("/refresh")
    @Operation(
            summary = "Refresh the access token",
            description = "Rotates the refresh token and issues a new access token."
    )
    public AuthDto.JwtAuthResponse refresh(@RequestBody AuthDto.RefreshJwtRequest request, HttpServletRequest httpRequest) {
        log.info("POST /api/v1/auth/refresh - Refreshing access token");
        return jwtAuthService.refresh(request, httpRequest);
    }

    @PostMapping("/logout")
    @Operation(
            summary = "Log out the current user",
            description = "Invalidates the current session and clears the session cookie."
    )
    public ResponseEntity<Void> logout(Authentication authentication) {
        jwtAuthService.logout(authentication);
        log.info("POST /api/v1/auth/logout - User logged out: {}", authentication.getName());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/forgot-password")
    @Operation(
            summary = "Send a password reset link/token by email",
            description = "Sends a reset token to the given email if an account exists for it. " +
                    "Does not confirm whether the email is registered, for security reasons."
    )
    public ResponseEntity<Map<String, String>> forgotPassword(
            @Valid @RequestBody AuthDto.ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        log.info("POST /api/v1/auth/forgot-password - Reset requested for: {}", request.email());
        return ResponseEntity.ok(Map.of("message",
                "Si un compte existe avec cet email, un lien de réinitialisation a été envoyé."));
    }

    @PostMapping("/reset-password")
    @Operation(
            summary = "Reset password using the token received by email",
            description = "Sets a new password after validating the code/token sent by email."
    )
    public ResponseEntity<Map<String, String>> resetPassword(
            @Valid @RequestBody AuthDto.ResetPasswordRequest request) {
        log.info("POST /api/v1/auth/reset-password - Password reset for: {}", request.newPassword());
        authService.resetMyPassword(request);
        return ResponseEntity.ok(Map.of("message", "Mot de passe modifié avec succès."));
    }
}