package com.clothingretail.auth;

import com.clothingretail.auth.dto.AuthResponse;
import com.clothingretail.auth.dto.LoginRequest;
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

    public AdminAuthController(AuthService authService, JwtService jwtService) {
        this.authService = authService;
        this.jwtService = jwtService;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResult<AuthResponse> result = authService.adminLogin(request);
        ResponseCookie cookie = ResponseCookie.from(AuthController.REFRESH_COOKIE_NAME, result.rawRefreshToken())
                .httpOnly(true)
                .secure(false)
                .sameSite("Lax")
                .path("/")
                .maxAge(jwtService.getRefreshTokenTtlSeconds())
                .build();
        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, cookie.toString()).body(result.body());
    }
}
