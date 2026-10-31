package com.ambdulghaffar.springstarterapi.config.keycloak;

import com.ambdulghaffar.springstarterapi.entity.User;
import com.ambdulghaffar.springstarterapi.entity.enums.UserEnum;
import com.ambdulghaffar.springstarterapi.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class JitProvisioningFilter extends OncePerRequestFilter {

    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        Authentication auth =
                SecurityContextHolder.getContext().getAuthentication();

        if (auth instanceof JwtAuthenticationToken jwtAuth) {

            Jwt jwt = jwtAuth.getToken();

            String email = jwt.getClaimAsString("email");

            if (email != null && !userRepository.existsByEmail(email)) {

                User newUser = User.builder()
                        .email(email)
                        .password(null)
                        .firstName(jwt.getClaimAsString("given_name"))
                        .lastName(jwt.getClaimAsString("family_name"))
                        .role(extractRole(jwt))
                        .status(UserEnum.UserStatus.ACTIVE)
                        .build();

                userRepository.save(newUser);
            }
        }

        filterChain.doFilter(request, response);
    }

    @SuppressWarnings("unchecked")
    private UserEnum.UserRole extractRole(Jwt jwt) {

        Map<String, Object> realmAccess =
                jwt.getClaim("realm_access");

        if (realmAccess == null) {
            return UserEnum.UserRole.USER;
        }

        Object rolesObject = realmAccess.get("roles");

        if (!(rolesObject instanceof Collection<?> roles)) {
            return UserEnum.UserRole.USER;
        }

        return roles.stream()
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .map(String::toUpperCase)
                .map(this::tryParseRole)
                .filter(role -> role != null)
                .findFirst()
                .orElse(UserEnum.UserRole.USER);
    }

    private UserEnum.UserRole tryParseRole(String value) {

        try {
            return UserEnum.UserRole.valueOf(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}