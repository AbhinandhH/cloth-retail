package com.clothingretail.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import java.util.Set;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

    // Same shape of secret as the app's local-dev fallback (base64-decodable, >= 256 bits for HS256).
    private static final String SECRET = "dGVzdC1vbmx5LWp3dC1zZWNyZXQtdGhhdC1pcy1sb25nLWVub3VnaC1mb3ItaG1hYy0yNTY=";

    @Test
    void issuesAndParsesAccessToken() {
        JwtService jwtService = new JwtService(SECRET, 15, 30);

        String token = jwtService.generateAccessToken(42L, "user@example.com", Set.of(RoleName.CUSTOMER));
        Claims claims = jwtService.parseClaims(token);

        assertThat(jwtService.extractUserId(claims)).isEqualTo(42L);
        assertThat(claims.get("email")).isEqualTo("user@example.com");
        assertThat(jwtService.extractRoles(claims)).containsExactly("CUSTOMER");
        assertThat(jwtService.isRefreshToken(claims)).isFalse();
        assertThat(jwtService.isValid(token)).isTrue();
        assertThat(jwtService.isExpired(token)).isFalse();
    }

    @Test
    void issuesAndParsesRefreshToken() {
        JwtService jwtService = new JwtService(SECRET, 15, 30);

        String token = jwtService.generateRefreshToken(7L);
        Claims claims = jwtService.parseClaims(token);

        assertThat(jwtService.extractUserId(claims)).isEqualTo(7L);
        assertThat(jwtService.isRefreshToken(claims)).isTrue();
        assertThat(jwtService.isValid(token)).isTrue();
    }

    @Test
    void expiredAccessTokenIsRejected() {
        // TTL of 0 minutes means the token's expiry is effectively "now", so it reads as expired almost immediately.
        JwtService jwtService = new JwtService(SECRET, 0, 30);
        String token = jwtService.generateAccessToken(1L, "a@b.com", Set.of(RoleName.CUSTOMER));

        // Give the clock a moment to move past the token's expiry.
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        assertThat(jwtService.isExpired(token)).isTrue();
        assertThat(jwtService.isValid(token)).isFalse();
        assertThrows(ExpiredJwtException.class, () -> jwtService.parseClaims(token));
    }

    @Test
    void tokenSignedWithDifferentSecretIsInvalid() {
        JwtService jwtService = new JwtService(SECRET, 15, 30);
        JwtService otherJwtService = new JwtService(
                "b3RoZXItdGVzdC1zZWNyZXQtdGhhdC1pcy1hbHNvLWxvbmctZW5vdWdoLWZvci1obWFj", 15, 30);

        String token = otherJwtService.generateAccessToken(1L, "a@b.com", Set.of(RoleName.CUSTOMER));

        assertThat(jwtService.isValid(token)).isFalse();
    }

    @Test
    void hashTokenIsDeterministicAndDiffersBetweenTokens() {
        JwtService jwtService = new JwtService(SECRET, 15, 30);
        String tokenA = jwtService.generateRefreshToken(1L);
        String tokenB = jwtService.generateRefreshToken(2L);

        assertThat(jwtService.hashToken(tokenA)).isEqualTo(jwtService.hashToken(tokenA));
        assertThat(jwtService.hashToken(tokenA)).isNotEqualTo(jwtService.hashToken(tokenB));
    }
}
