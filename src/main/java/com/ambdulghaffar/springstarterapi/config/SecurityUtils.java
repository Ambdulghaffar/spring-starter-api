package com.ambdulghaffar.springstarterapi.config;

import com.ambdulghaffar.springstarterapi.entity.User;
import com.ambdulghaffar.springstarterapi.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component("securityUtils")
@RequiredArgsConstructor
public class SecurityUtils {

    private final UserRepository userRepository;

    public User getCurrentUser() {
        Authentication auth = getAuthentication();

        return userRepository.findByEmail(auth.getName())
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.UNAUTHORIZED,
                                "Aucun utilisateur trouvé avec l'email: " + auth.getName()
                        )
                );
    }

    public Long getCurrentUserIdOrNull() {
        Authentication auth = SecurityContextHolder
                .getContext()
                .getAuthentication();

        if (auth == null
                || !auth.isAuthenticated()
                || auth instanceof AnonymousAuthenticationToken) {
            return null;
        }

        return userRepository.findByEmail(auth.getName())
                .map(User::getId)
                .orElse(null);
    }

    public Long getCurrentUserId() {
        return getCurrentUser().getId();
    }

    public boolean hasRole(String role) {
        Authentication auth = SecurityContextHolder
                .getContext()
                .getAuthentication();

        return auth != null
                && auth.isAuthenticated()
                && auth.getAuthorities()
                .stream()
                .anyMatch(authority ->
                        authority.getAuthority()
                                .equals("ROLE_" + role)
                );
    }


    private Authentication getAuthentication() {
        Authentication auth = SecurityContextHolder
                .getContext()
                .getAuthentication();

        if (auth == null || !auth.isAuthenticated()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Aucun utilisateur connecté"
            );
        }

        return auth;
    }
}