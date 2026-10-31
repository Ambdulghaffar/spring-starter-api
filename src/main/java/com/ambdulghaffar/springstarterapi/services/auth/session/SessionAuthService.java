package com.ambdulghaffar.springstarterapi.services.auth.session;

import com.ambdulghaffar.springstarterapi.dto.auth.AuthDto;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public interface SessionAuthService {
    void login(AuthDto.LoginRequest request,
                           HttpServletRequest httpRequest,
                           HttpServletResponse httpResponse);
    void logout(HttpServletRequest httpRequest,
                HttpServletResponse httpResponse);
}