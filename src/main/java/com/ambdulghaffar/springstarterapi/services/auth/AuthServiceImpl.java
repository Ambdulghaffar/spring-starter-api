package com.ambdulghaffar.springstarterapi.services.auth;

import com.ambdulghaffar.springstarterapi.dto.UserDto;
import com.ambdulghaffar.springstarterapi.dto.auth.AuthDto;
import com.ambdulghaffar.springstarterapi.entity.EmailToken;
import com.ambdulghaffar.springstarterapi.entity.User;
import com.ambdulghaffar.springstarterapi.entity.enums.UserEnum;
import com.ambdulghaffar.springstarterapi.mapper.UserMapper;
import com.ambdulghaffar.springstarterapi.repository.EmailTokenRepository;
import com.ambdulghaffar.springstarterapi.repository.UserRepository;
import com.ambdulghaffar.springstarterapi.services.MailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private static final Duration RESET_TTL = Duration.ofMinutes(30);
    private static final Duration CONFIRMATION_TTL = Duration.ofHours(24);

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final EmailTokenRepository emailTokenRepository;
    private final MailService mailService;
    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    @Transactional
    public UserDto.Response register(AuthDto.RegisterRequest request) {
        log.debug("Registration attempt for email: {}", request.email());

        if (userRepository.findByEmail(request.email()).isPresent()) {
            log.warn("Registration failed - user already exists: {}", request.email());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "L'utilisateur avec l'email " + request.email() + " existe déjà.");
        }

        User user = User.builder()
                .firstName(request.firstName()).lastName(request.lastName())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .phoneNumber(request.phoneNumber())
                .role(UserEnum.UserRole.USER).status(UserEnum.UserStatus.PENDING_VERIFICATION)
                .build();

        User savedUser = userRepository.save(user);

        String token = issueToken(savedUser, UserEnum.EmailTokenType.EMAIL_CONFIRMATION, CONFIRMATION_TTL);
        mailService.sendVerificationLink(savedUser.getEmail(), token);

        log.info("User registered, confirmation email sent. id: {}", savedUser.getId());

        return userMapper.toDto(savedUser);
    }

    @Override
    @Transactional
    public void verifyEmail(AuthDto.VerifyEmailRequest request) {
        User user = consumeToken(request.token(), UserEnum.EmailTokenType.EMAIL_CONFIRMATION).getUser();

        // Seul un compte en attente est activé : un compte BLOCKED/BANNED/INACTIVE
        // (désactivé par un admin) ne doit pas se réactiver via un vieux lien.
        if (user.getStatus() == UserEnum.UserStatus.PENDING_VERIFICATION) {
            user.setStatus(UserEnum.UserStatus.ACTIVE);
        }
        log.info("Email confirmed for user id: {}", user.getId());
    }

    @Override
    @Transactional
    public void resendVerification(AuthDto.ResendVerificationRequest request) {
        userRepository.findByEmail(request.email())
                .filter(u -> u.getStatus() == UserEnum.UserStatus.PENDING_VERIFICATION)
                .ifPresent(u -> {
                    String token = issueToken(u, UserEnum.EmailTokenType.EMAIL_CONFIRMATION, CONFIRMATION_TTL);
                    mailService.sendVerificationLink(u.getEmail(), token);
                    log.info("Confirmation email re-sent for user id: {}", u.getId());
                });
    }

    @Override
    @Transactional
    public void forgotPassword(AuthDto.ForgotPasswordRequest request) {
        userRepository.findByEmail(request.email()).ifPresentOrElse(user -> {
            String token = issueToken(user, UserEnum.EmailTokenType.PASSWORD_RESET, RESET_TTL);
            mailService.sendResetLink(user.getEmail(), token);
            log.info("Password reset token issued for user id: {}", user.getId());
        }, () -> log.warn("Password reset requested for unknown email"));
    }

    @Override
    @Transactional
    public void resetMyPassword(AuthDto.ResetPasswordRequest request) {
        User user = consumeToken(request.token(), UserEnum.EmailTokenType.PASSWORD_RESET).getUser();
        user.setPassword(passwordEncoder.encode(request.newPassword()));

        // TODO : invalider ici les sessions / refresh tokens existants de l'utilisateur

        log.info("Password reset successfully for user id: {}", user.getId());
    }

    // ---------------------------------------------------------------- helpers

    private String issueToken(User user, UserEnum.EmailTokenType type, Duration ttl) {
        emailTokenRepository.deleteByUserAndType(user, type); // un seul lien actif par type

        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        emailTokenRepository.save(new EmailToken(hash(token), user, type, Instant.now().plus(ttl)));
        return token;
    }

    private EmailToken consumeToken(String rawToken, UserEnum.EmailTokenType type) {
        EmailToken token = emailTokenRepository.findByTokenHashAndType(hash(rawToken), type)
                .filter(EmailToken::isUsable)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "Le lien est invalide ou a expiré."));
        token.setUsedAt(Instant.now());
        return token;
    }

    private static String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}