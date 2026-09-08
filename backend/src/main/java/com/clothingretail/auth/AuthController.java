package com.clothingretail.auth;

import com.clothingretail.auth.dto.AuthResponse;
import com.clothingretail.auth.dto.LoginRequest;
import com.clothingretail.auth.dto.RegisterRequest;
import com.clothingretail.auth.dto.TokenResponse;
import com.clothingretail.auth.dto.UserSummary;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    public static final String REFRESH_COOKIE_NAME = "refreshToken";

    private final AuthService authService;
    private final JwtService jwtService;
    private final RefreshCookieFactory refreshCookieFactory;

    public AuthController(AuthService authService, JwtService jwtService, RefreshCookieFactory refreshCookieFactory) {
        this.authService = authService;
        this.jwtService = jwtService;
        this.refreshCookieFactory = refreshCookieFactory;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResult<AuthResponse> result = authService.register(request);
        return withRefreshCookie(result);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResult<AuthResponse> result = authService.login(request);
        return withRefreshCookie(result);
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(
            @CookieValue(name = REFRESH_COOKIE_NAME, required = false) String refreshToken) {
        AuthResult<TokenResponse> result = authService.refresh(refreshToken);
        return withRefreshCookie(result);
    }

    /** Authenticated-only "who am I" check - handy for the frontend to validate a stored access token / hydrate session state. */
    @GetMapping("/me")
    public UserSummary me(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return authService.getCurrentUser(userId);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@CookieValue(name = REFRESH_COOKIE_NAME, required = false) String refreshToken) {
        authService.logout(refreshToken);
        return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE, refreshCookieFactory.clear().toString()).build();
    }

    private <T> ResponseEntity<T> withRefreshCookie(AuthResult<T> result) {
        ResponseCookie cookie = refreshCookieFactory.build(result.rawRefreshToken(), jwtService.getRefreshTokenTtlSeconds());
        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, cookie.toString()).body(result.body());
    }
}
