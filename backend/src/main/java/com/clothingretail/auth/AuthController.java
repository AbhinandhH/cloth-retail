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

    public AuthController(AuthService authService, JwtService jwtService) {
        this.authService = authService;
        this.jwtService = jwtService;
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
        ResponseCookie cleared = ResponseCookie.from(REFRESH_COOKIE_NAME, "")
                .httpOnly(true)
                .secure(false)
                .sameSite("Lax")
                .path("/")
                .maxAge(0)
                .build();
        return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE, cleared.toString()).build();
    }

    private <T> ResponseEntity<T> withRefreshCookie(AuthResult<T> result) {
        ResponseCookie cookie = ResponseCookie.from(REFRESH_COOKIE_NAME, result.rawRefreshToken())
                .httpOnly(true)
                // NOTE: secure=false so the cookie also works over plain http://localhost in
                // local dev. Set to true once the app is served over https in production.
                .secure(false)
                .sameSite("Lax")
                .path("/")
                .maxAge(jwtService.getRefreshTokenTtlSeconds())
                .build();
        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, cookie.toString()).body(result.body());
    }
}
