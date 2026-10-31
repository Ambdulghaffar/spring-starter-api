package com.ambdulghaffar.springstarterapi.services.auth.session;

import com.ambdulghaffar.springstarterapi.dto.auth.AuthDto;
import com.ambdulghaffar.springstarterapi.repository.UserRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Profile("session")
@Slf4j
@Service
@RequiredArgsConstructor
public class SessionAuthServiceImpl implements SessionAuthService {

    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository; // <-- nouveau
    private final UserRepository userRepository;

    @Override
    public void login(
            AuthDto.LoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.email(),
                        request.password()
                )
        );

        var user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "INVALID_CREDENTIALS"
                ));

        switch (user.getStatus()) {
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
                log.info("Login authorized for user: {}", auth.getName());
            }
        }

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);

        securityContextRepository.saveContext(
                context,
                httpRequest,
                httpResponse
        );

        log.info("User logged in: {}", auth.getName());
    }

    @Override
    public void logout(HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        boolean isAuthenticated = auth != null
                && auth.isAuthenticated()
                && !(auth instanceof AnonymousAuthenticationToken);

        if (!isAuthenticated) {
            log.warn("Logout attempted without a valid session");
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Tu n'es pas connecté.");
        }

        String email = auth.getName();

        // Invalidate the user's session
        HttpSession session = httpRequest.getSession(false);
        if (session != null) {
            session.invalidate();
        }

        SecurityContextHolder.clearContext();

        Cookie cookie = new Cookie("SESSION", null);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        cookie.setHttpOnly(true);
        httpResponse.addCookie(cookie);

        log.info("User logged out: {}", email);
    }
}