package com.ambdulghaffar.springstarterapi.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Profile;

@ConfigurationProperties(prefix = "jwt")
@Profile("jwt")
public record JwtProperties(
        String secret,
        long accessTokenExpiration,
        long refreshTokenExpiration,
        long refreshTokenReuseGracePeriod
) {}