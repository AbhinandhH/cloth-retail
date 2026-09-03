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
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("email", email)
                .claim(CLAIM_ROLES, roleNames)
                .claim(CLAIM_TYPE, TYPE_ACCESS)
                .id(UUID.randomUUID().toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(accessTokenTtlSeconds)))
                .signWith(key)
                .compact();
    }

    public String generateRefreshToken(Long userId) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim(CLAIM_TYPE, TYPE_REFRESH)
                .id(UUID.randomUUID().toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(refreshTokenTtlSeconds)))
                .signWith(key)
                .compact();
    }

    /** Parses and signature/expiry-validates a token. Throws {@link JwtException} (or a subtype) if invalid. */
    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isValid(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException ex) {
            return false;
        }
    }

    public boolean isExpired(String token) {
        try {
            parseClaims(token);
            return false;
        } catch (ExpiredJwtException ex) {
            return true;
        } catch (JwtException | IllegalArgumentException ex) {
            return true;
        }
    }

    public Long extractUserId(Claims claims) {
        return Long.valueOf(claims.getSubject());
    }

    @SuppressWarnings("unchecked")
    public Set<String> extractRoles(Claims claims) {
        Object raw = claims.get(CLAIM_ROLES);
        if (raw instanceof List<?> list) {
            return list.stream().map(String::valueOf).collect(Collectors.toSet());
        }
        return Set.of();
    }

    public boolean isRefreshToken(Claims claims) {
        return TYPE_REFRESH.equals(claims.get(CLAIM_TYPE, String.class));
    }

    /** SHA-256 hex digest, used so refresh tokens are never persisted in plaintext. */
    public String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
