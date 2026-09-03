package com.clothingretail.auth.dto;

/** Response for POST /api/auth/refresh - no user object, matches the API contract exactly. */
public record TokenResponse(String accessToken, String tokenType, long expiresIn) {
    public static TokenResponse of(String accessToken, long expiresIn) {
        return new TokenResponse(accessToken, "Bearer", expiresIn);
    }
}
