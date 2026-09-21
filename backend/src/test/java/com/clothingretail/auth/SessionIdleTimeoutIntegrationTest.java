package com.clothingretail.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.clothingretail.auth.repository.RefreshTokenRepository;
import com.clothingretail.auth.service.JwtService;
import com.clothingretail.siteconfig.SiteConfiguration;
import com.clothingretail.siteconfig.repository.SiteConfigurationRepository;
import jakarta.servlet.http.Cookie;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Covers the admin-configurable idle session timeout (SiteConfiguration#idleTimeoutMinutes),
 * enforced server-side in AuthServiceImpl#refresh against RefreshToken#lastUsedAt - a refresh
 * presented after the configured window has elapsed since the token was last used is rejected
 * instead of rotated, regardless of the token's own (much longer) expiresAt.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SessionIdleTimeoutIntegrationTest {

    private static final String REFRESH_COOKIE_NAME = "refreshToken";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private SiteConfigurationRepository siteConfigurationRepository;

    @Autowired
    private JwtService jwtService;

    @AfterEach
    void restoreDefaultIdleTimeout() {
        SiteConfiguration config = siteConfigurationRepository.findById(SiteConfiguration.SINGLETON_ID).orElseThrow();
        config.setIdleTimeoutMinutes(30);
        siteConfigurationRepository.save(config);
    }

    @Test
    void refreshSucceedsWithinTheIdleWindowButFailsOnceItHasElapsed() throws Exception {
        SiteConfiguration config = siteConfigurationRepository.findById(SiteConfiguration.SINGLETON_ID).orElseThrow();
        config.setIdleTimeoutMinutes(10);
        siteConfigurationRepository.save(config);

        String email = "idle-timeout-" + UUID.randomUUID() + "@example.com";
        String registerBody = """
                {"fullName":"Idle Timeout Customer","email":"%s","mobileNumber":"9876500001","password":"Password123!"}
                """.formatted(email);

        MvcResult registerResult = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody))
                .andExpect(status().isOk())
                .andReturn();

        Cookie refreshCookie = registerResult.getResponse().getCookie(REFRESH_COOKIE_NAME);
        assertThat(refreshCookie).isNotNull();

        // Well within the 10-minute idle window configured above - refresh succeeds and rotates
        // to a new token.
        MvcResult refreshResult = mockMvc.perform(post("/api/auth/refresh").cookie(refreshCookie))
                .andExpect(status().isOk())
                .andReturn();
        Cookie rotatedCookie = refreshResult.getResponse().getCookie(REFRESH_COOKIE_NAME);
        assertThat(rotatedCookie).isNotNull();

        // Directly backdate the rotated token's lastUsedAt past the 10-minute window, simulating
        // a session that's been idle - no code path does this in production, it just stands in
        // for time actually passing.
        RefreshToken stored = refreshTokenRepository
                .findByTokenHash(jwtService.hashToken(rotatedCookie.getValue()))
                .orElseThrow();
        stored.setLastUsedAt(Instant.now().minus(11, ChronoUnit.MINUTES));
        refreshTokenRepository.save(stored);

        // Same token, but now idle-expired per the configured window - refresh must be rejected
        // even though the token's own (30-day) expiresAt is nowhere close.
        mockMvc.perform(post("/api/auth/refresh").cookie(rotatedCookie))
                .andExpect(status().isUnauthorized());
    }
}
