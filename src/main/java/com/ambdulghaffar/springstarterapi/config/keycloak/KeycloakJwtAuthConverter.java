package com.ambdulghaffar.springstarterapi.config.keycloak;

import com.ambdulghaffar.springstarterapi.entity.enums.UserEnum;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class KeycloakJwtAuthConverter
        implements Converter<Jwt, AbstractAuthenticationToken> {

    private static final Set<String> KNOWN_ROLES =
            Arrays.stream(UserEnum.UserRole.values())
                    .map(Enum::name)
                    .collect(Collectors.toSet());

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {

        Collection<GrantedAuthority> authorities =
                extractAuthorities(jwt);

        String principalName =
                jwt.getClaimAsString("email");

        return new JwtAuthenticationToken(
                jwt,
                authorities,
                principalName
        );
    }

    @SuppressWarnings("unchecked")
    private Collection<GrantedAuthority> extractAuthorities(Jwt jwt) {

        Map<String, Object> realmAccess =
                jwt.getClaim("realm_access");

        if (realmAccess == null) {
            return List.of();
        }

        Object rolesObject = realmAccess.get("roles");

        if (!(rolesObject instanceof Collection<?> roles)) {
            return List.of();
        }

        return roles.stream()
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .map(String::toUpperCase)
                .filter(KNOWN_ROLES::contains)
                .map(role ->
                        new SimpleGrantedAuthority("ROLE_" + role)
                )
                .collect(Collectors.toList());
    }
}