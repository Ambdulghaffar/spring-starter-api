package com.ambdulghaffar.springstarterapi.services.auth.jwt;

import com.ambdulghaffar.springstarterapi.config.jwt.service.JwtAccessTokenServiceImpl;
import com.ambdulghaffar.springstarterapi.config.jwt.service.JwtRefreshTokenService;
import com.ambdulghaffar.springstarterapi.dto.auth.AuthDto;
import com.ambdulghaffar.springstarterapi.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Profile("jwt")
@Slf4j
@Service
@RequiredArgsConstructor
public class JwtAuthServiceImpl implements JwtAuthService {
    private final AuthenticationManager authenticationManager;
    private final JwtAccessTokenServiceImpl jwtService;
    private final JwtRefreshTokenService refreshTokenService;
    private final UserDetailsService userDetailsService;
    private final UserRepository userRepository;

    @Override
    public AuthDto.JwtAuthResponse login(
            AuthDto.LoginRequest request,
            HttpServletRequest httpRequest
    ) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.email(),
                        request.password()
                )
        );

        var userEntity = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "INVALID_CREDENTIALS"
                ));

        switch (userEntity.getStatus()) {
            case PENDING_VERIFICATION ->
                    throw new ResponseStatusException(
                            HttpStatus.FORBIDDEN,
                            "EMAIL_NOT_VERIFIED"
                    );

            case BLOCKED ->
                    throw new ResponseStatusException(
                            HttpStatus.FORBIDDEN,
                            "ACCOUNT_BLOCKED"
                    );

            case DELETED ->
                    throw new ResponseStatusException(
                            HttpStatus.FORBIDDEN,
                            "ACCOUNT_DELETED"
                    );

            case ACTIVE -> {
                log.info("Login authorized for user: {}", userEntity.getEmail());
            }
        }

        UserDetails user = (UserDetails) authentication.getPrincipal();
        log.info("Generated access token for user: {}", user.getUsername());
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = refreshTokenService.issue(
                user.getUsername(),
                httpRequest
        );
        log.info("User logged in: {}", user.getUsername());
        return new AuthDto.JwtAuthResponse(accessToken, refreshToken);
    }

    @Override
    public AuthDto.JwtAuthResponse refresh(AuthDto.RefreshJwtRequest request, HttpServletRequest httpRequest) {
        String userId = refreshTokenService.rotate(request.refreshToken(), httpRequest);
        UserDetails user = userDetailsService.loadUserByUsername(userId);

        String newAccessToken = jwtService.generateAccessToken(user);
        String newRefreshToken = refreshTokenService.issue(userId, httpRequest);

        log.info("User refreshed access token: {}", user.getUsername());
        return new AuthDto.JwtAuthResponse(newAccessToken, newRefreshToken);
    }

    @Override
    public void logout(Authentication authentication) {
        log.info("User logged out: {}", authentication.getName());
        refreshTokenService.revokeAllForUser(authentication.getName());
    }
}
