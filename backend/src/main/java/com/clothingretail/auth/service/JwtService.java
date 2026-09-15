package com.clothingretail.auth.service;

import com.clothingretail.auth.RoleName;
import io.jsonwebtoken.Claims;
import java.util.Set;

/**
 * Issues, parses and validates the two kinds of JWT this app uses:
 * <ul>
 *   <li>access tokens - short-lived, sent in the Authorization header, carry user id + roles</li>
 *   <li>refresh tokens - longer-lived, opaque to the client (sent only via the httpOnly cookie),
 *       whose hash is checked against RefreshToken rows in the DB so they can be revoked</li>
 * </ul>
 */
public interface JwtService {

    long getAccessTokenTtlSeconds();

    long getRefreshTokenTtlSeconds();

    String generateAccessToken(Long userId, String email, Set<RoleName> roles);

    String generateRefreshToken(Long userId);

    /** Parses and signature/expiry-validates a token. Throws a JwtException (or a subtype) if invalid. */
    Claims parseClaims(String token);

    boolean isValid(String token);

    boolean isExpired(String token);

    Long extractUserId(Claims claims);

    Set<String> extractRoles(Claims claims);

    boolean isRefreshToken(Claims claims);

    /** SHA-256 hex digest, used so refresh tokens are never persisted in plaintext. */
    String hashToken(String token);
}
