package com.clothingretail.auth.controller;

import com.clothingretail.auth.AuthResult;
import com.clothingretail.auth.dto.AuthResponse;
import com.clothingretail.auth.dto.LoginRequest;
import com.clothingretail.auth.service.AuthService;
import com.clothingretail.auth.service.JwtService;
import com.clothingretail.auth.service.RefreshCookieFactory;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/auth")
public class AdminAuthController {

    private final AuthService authService;
    private final JwtService jwtService;
    private final RefreshCookieFactory refreshCookieFactory;

    public AdminAuthController(AuthService authService, JwtService jwtService, RefreshCookieFactory refreshCookieFactory) {
        this.authService = authService;
        this.jwtService = jwtService;
        this.refreshCookieFactory = refreshCookieFactory;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResult<AuthResponse> result = authService.adminLogin(request);
        ResponseCookie cookie = refreshCookieFactory.build(result.rawRefreshToken(), jwtService.getRefreshTokenTtlSeconds());
        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, cookie.toString()).body(result.body());
    }
}
