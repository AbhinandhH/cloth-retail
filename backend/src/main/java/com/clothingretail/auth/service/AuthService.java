package com.clothingretail.auth.service;

import com.clothingretail.auth.AuthResult;
import com.clothingretail.auth.dto.AuthResponse;
import com.clothingretail.auth.dto.CreateAdminRequest;
import com.clothingretail.auth.dto.LoginRequest;
import com.clothingretail.auth.dto.RegisterRequest;
import com.clothingretail.auth.dto.ResendOtpRequest;
import com.clothingretail.auth.dto.TokenResponse;
import com.clothingretail.auth.dto.UserSummary;
import com.clothingretail.auth.dto.VerificationStatusResponse;
import com.clothingretail.auth.dto.VerifyOtpRequest;

public interface AuthService {

    AuthResult<VerificationStatusResponse> register(RegisterRequest request);

    AuthResult<VerificationStatusResponse> verifyOtp(VerifyOtpRequest request);

    void resendOtp(ResendOtpRequest request);

    AuthResult<AuthResponse> login(LoginRequest request);

    AuthResult<AuthResponse> adminLogin(LoginRequest request);

    AuthResult<TokenResponse> refresh(String rawRefreshToken);

    void logout(String rawRefreshToken);

    UserSummary createAdmin(CreateAdminRequest request);

    UserSummary getCurrentUser(Long userId);
}
