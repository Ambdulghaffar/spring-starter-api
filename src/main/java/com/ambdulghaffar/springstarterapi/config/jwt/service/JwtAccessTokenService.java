package com.ambdulghaffar.springstarterapi.config.jwt.service;

import io.jsonwebtoken.Claims;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.function.Function;

public interface JwtAccessTokenService {

    String generateAccessToken(UserDetails userDetails);

    String generateRefreshToken(UserDetails userDetails);

    String extractUsername(String token);

    <T> T extractClaim(String token, Function<Claims, T> resolver);

    boolean isTokenValid(String token, UserDetails userDetails);
}