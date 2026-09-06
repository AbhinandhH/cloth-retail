package com.clothingretail.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.crypto.SecretKey;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Issues, parses and validates the two kinds of JWT this app uses:
 * <ul>
 *   <li>access tokens - short-lived, sent in the Authorization header, carry user id + roles</li>
 *   <li>refresh tokens - longer-lived, opaque to the client (sent only via the httpOnly cookie),
 *       whose hash is checked against {@link RefreshToken} rows in the DB so they can be revoked</li>
 * </ul>
 */
@Service
@Log4j2
public class JwtService {

    private static final String CLAIM_ROLES = "roles";
    private static final String CLAIM_TYPE = "type";
    private static final String TYPE_ACCESS = "access";
    private static final String TYPE_REFRESH = "refresh";

    private final SecretKey key;
    private final long accessTokenTtlSeconds;
    private final long refreshTokenTtlSeconds;

    public JwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.access-token-ttl-minutes:15}") long accessTokenTtlMinutes,
            @Value("${app.jwt.refresh-token-ttl-days:30}") long refreshTokenTtlDays) {
        this.key = Keys.hmacShaKeyFor(Base64.getDecoder().decode(secret));
        this.accessTokenTtlSeconds = Duration.ofMinutes(accessTokenTtlMinutes).toSeconds();
        this.refreshTokenTtlSeconds = Duration.ofDays(refreshTokenTtlDays).toSeconds();
    }

    public long getAccessTokenTtlSeconds() {
        return accessTokenTtlSeconds;
    }

    public long getRefreshTokenTtlSeconds() {
        return refreshTokenTtlSeconds;
    }

    public String generateAccessToken(Long userId, String email, Set<RoleName> roles) {
        Instant now = Instant.now();
        List<String> roleNames = roles.stream().map(Enum::name).collect(Collectors.toList());
        Instant expiresAt = now.plusSeconds(accessTokenTtlSeconds);
        String token = Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("email", email)
                .claim(CLAIM_ROLES, roleNames)
                .claim(CLAIM_TYPE, TYPE_ACCESS)
                .id(UUID.randomUUID().toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .signWith(key)
                .compact();
        log.info("[1035] Access token generated userId={} email={} roles={} expiresAt={}", userId, email, roleNames, expiresAt);
        return token;
    }

    public String generateRefreshToken(Long userId) {
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(refreshTokenTtlSeconds);
        String token = Jwts.builder()
                .subject(String.valueOf(userId))
                .claim(CLAIM_TYPE, TYPE_REFRESH)
                .id(UUID.randomUUID().toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .signWith(key)
                .compact();
        log.info("[1036] Refresh token generated userId={} expiresAt={}", userId, expiresAt);
        return token;
    }

    /** Parses and signature/expiry-validates a token. Throws {@link JwtException} (or a subtype) if invalid. */
    public Claims parseClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException | IllegalArgumentException ex) {
            log.error("[1037] Failed to parse/validate JWT: {}", ex.getMessage());
            throw ex;
        }
    }

    public boolean isValid(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException ex) {
            log.error("[1038] Token validity check failed: {}", ex.getMessage());
            return false;
        }
    }

    public boolean isExpired(String token) {
        try {
            parseClaims(token);
            return false;
        } catch (ExpiredJwtException ex) {
            log.info("[1039] Token expired, subject={}", ex.getClaims() != null ? ex.getClaims().getSubject() : null);
            return true;
        } catch (JwtException | IllegalArgumentException ex) {
            log.error("[1040] Token expiry check failed, malformed token: {}", ex.getMessage());
            return true;
        }
    }

    public Long extractUserId(Claims claims) {
        Long userId = Long.valueOf(claims.getSubject());
        log.info("[1041] Extracted userId={} from token claims", userId);
        return userId;
    }

    @SuppressWarnings("unchecked")
    public Set<String> extractRoles(Claims claims) {
        Object raw = claims.get(CLAIM_ROLES);
        Set<String> roles;
        if (raw instanceof List<?> list) {
            roles = list.stream().map(String::valueOf).collect(Collectors.toSet());
        } else {
            roles = Set.of();
        }
        log.info("[1042] Extracted roles={} from token claims", roles);
        return roles;
    }

    public boolean isRefreshToken(Claims claims) {
        String tokenType = claims.get(CLAIM_TYPE, String.class);
        boolean isRefresh = TYPE_REFRESH.equals(tokenType);
        log.info("[1043] Token type check tokenType={} isRefresh={}", tokenType, isRefresh);
        return isRefresh;
    }

    /** SHA-256 hex digest, used so refresh tokens are never persisted in plaintext. */
    public String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            String hex = HexFormat.of().formatHex(hash);
            log.info("[1044] Token hashed (sha256 hex, not logging raw token)");
            return hex;
        } catch (NoSuchAlgorithmException e) {
            log.error("[1045] Token hashing failed, SHA-256 unavailable: {}", e.getMessage());
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
