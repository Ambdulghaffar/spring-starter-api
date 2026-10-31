package com.ambdulghaffar.springstarterapi.services.auth.jwt;

import com.ambdulghaffar.springstarterapi.dto.auth.AuthDto;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;

public interface JwtAuthService {
    AuthDto.JwtAuthResponse login(AuthDto.LoginRequest request,
                                  HttpServletRequest httpRequest);
    AuthDto.JwtAuthResponse refresh(AuthDto.RefreshJwtRequest request,
                                    HttpServletRequest httpRequest);
    void logout(Authentication authentication);
}
