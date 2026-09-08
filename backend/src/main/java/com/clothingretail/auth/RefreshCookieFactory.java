package com.clothingretail.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

/**
 * Builds the httpOnly refresh-token cookie with a single, centrally configured
 * SameSite/Secure policy, rather than each of the three call sites (register,
 * login, refresh, admin login, logout) hardcoding its own copy that could drift.
 *
 * Defaults suit local http://localhost dev (Lax, non-secure). A deployment where
 * the frontend and backend are on two different origins - e.g. two separate ngrok
 * tunnels for a public demo - needs SameSite=None paired with Secure=true instead,
 * since a plain Lax cookie is never sent on cross-site XHR/fetch requests, and
 * SameSite=None is only honored by browsers when Secure is also set. Override via
 * COOKIE_SECURE=true and COOKIE_SAME_SITE=None (ngrok serves https, so Secure
 * still works). Don't flip these for same-origin local dev - a Secure cookie is
 * silently rejected over plain http.
 */
@Component
public class RefreshCookieFactory {

    private final boolean secure;
    private final String sameSite;

    public RefreshCookieFactory(
            @Value("${app.auth.cookie-secure:false}") boolean secure,
            @Value("${app.auth.cookie-same-site:Lax}") String sameSite) {
        this.secure = secure;
        this.sameSite = sameSite;
    }

    public ResponseCookie build(String value, long maxAgeSeconds) {
        return ResponseCookie.from(AuthController.REFRESH_COOKIE_NAME, value)
                .httpOnly(true)
                .secure(secure)
                .sameSite(sameSite)
                .path("/")
                .maxAge(maxAgeSeconds)
                .build();
    }

    public ResponseCookie clear() {
        return build("", 0);
    }
}
