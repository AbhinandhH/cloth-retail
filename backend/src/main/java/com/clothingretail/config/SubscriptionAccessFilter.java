package com.clothingretail.config;

import com.clothingretail.common.HttpErrorResponses;
import com.clothingretail.subscription.SubscriptionBilling;
import com.clothingretail.subscription.repository.SubscriptionBillingRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * The site-wide subscription lockout - registered in SecurityConfig with
 * {@code addFilterAfter(this, JwtAuthenticationFilter.class)}, specifically so it runs AFTER JWT
 * auth has already populated {@link SecurityContextHolder} (letting this filter read the caller's
 * roles) but BEFORE Spring Security's own {@code authorizeHttpRequests}/{@code @PreAuthorize}
 * machinery, so a locked-out request never reaches a controller at all.
 *
 * <p>Deliberately a much NARROWER allowlist than SecurityConfig's own {@code permitAll()} list:
 * once locked, even the ordinarily-public storefront GETs (products, categories, /configuration,
 * ...) are blocked too - "the site won't be accessible for anyone" means literally that, not just
 * authenticated routes. SUPER_ADMIN is the one exemption, checked by role rather than by any
 * "am I the bootstrap account" flag - {@code AuthServiceImpl.createAdmin}'s role whitelist can
 * never mint a second SUPER_ADMIN (see its own doc comment), so the role check alone is already
 * exactly "is this the software owner".
 *
 * <p>Reads {@link SubscriptionBilling} fresh from the database on every request - no cache. A
 * cached "is locked" flag would mean a real payment doesn't unlock the site until the cache
 * expires, which is exactly backwards for what this check exists to do.
 */
public class SubscriptionAccessFilter extends OncePerRequestFilter {

    private final SubscriptionBillingRepository subscriptionBillingRepository;

    public SubscriptionAccessFilter(SubscriptionBillingRepository subscriptionBillingRepository) {
        this.subscriptionBillingRepository = subscriptionBillingRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (isAllowlisted(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        if (isSuperAdmin()) {
            filterChain.doFilter(request, response);
            return;
        }

        boolean locked = subscriptionBillingRepository.findById(SubscriptionBilling.SINGLETON_ID)
                .map(SubscriptionBilling::isLocked)
                .orElse(false);
        if (!locked) {
            filterChain.doFilter(request, response);
            return;
        }

        HttpErrorResponses.write(
                response,
                402,
                "Payment Required",
                "This store is temporarily unavailable. Please contact the site owner.",
                request.getRequestURI());
    }

    private boolean isAllowlisted(HttpServletRequest request) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String path = request.getRequestURI();
        String method = request.getMethod();
        if ("POST".equalsIgnoreCase(method) && "/api/admin/auth/login".equals(path)) {
            return true;
        }
        // Mints a new access token from the httpOnly refresh cookie - grants no access to any
        // protected resource by itself, since every other endpoint is still re-checked by this
        // same filter on its own request. Must stay reachable so an already-locked-out SUPER_ADMIN
        // session (or any session) can silently refresh without that refresh itself 402ing.
        if ("POST".equalsIgnoreCase(method) && "/api/auth/refresh".equals(path)) {
            return true;
        }
        if ("POST".equalsIgnoreCase(method) && "/api/subscription/webhook".equals(path)) {
            return true;
        }
        return false;
    }

    private boolean isSuperAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return false;
        }
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            if ("ROLE_SUPER_ADMIN".equals(authority.getAuthority())) {
                return true;
            }
        }
        return false;
    }
}
