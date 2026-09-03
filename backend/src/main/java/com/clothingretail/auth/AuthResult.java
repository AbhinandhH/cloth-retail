package com.clothingretail.auth;

/** Internal carrier so the controller can set the refresh-token cookie without the raw token ever appearing in a JSON body. */
public record AuthResult<T>(T body, String rawRefreshToken) {}
