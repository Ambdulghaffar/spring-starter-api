package com.ambdulghaffar.springstarterapi.config.jwt.service;

import jakarta.servlet.http.HttpServletRequest;

public interface JwtRefreshTokenService {

    String issue(String userId, HttpServletRequest request);

    String rotate(String rawToken, HttpServletRequest request);

    void revokeAllForUser(String userId);
}