package com.ambdulghaffar.springstarterapi.services.auth;

import com.ambdulghaffar.springstarterapi.dto.UserDto;
import com.ambdulghaffar.springstarterapi.dto.auth.AuthDto;

public interface AuthService {
    UserDto.Response register(AuthDto.RegisterRequest request);
    void verifyEmail(AuthDto.VerifyEmailRequest request);
    void resendVerification(AuthDto.ResendVerificationRequest request);
    void forgotPassword(AuthDto.ForgotPasswordRequest request);
    void resetMyPassword(AuthDto.ResetPasswordRequest request);
}
