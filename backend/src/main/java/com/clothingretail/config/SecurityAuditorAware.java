package com.clothingretail.config;

import java.util.Optional;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Feeds {@code @CreatedBy`/`@LastModifiedBy} (see AuditableMasterEntity) from the current
 * authenticated user id - the same principal shape JwtAuthenticationFilter puts in the
 * SecurityContext (a plain {@code Long}, see AdminInventoryController#actingUserId for the
 * identical resolution used elsewhere in the app).
 *
 * <p>Returns {@code Optional.empty()} whenever there's no authenticated user - notably
 * during Flyway migrations/seed data, which run before Spring Security is even in the
 * picture, so this bean isn't invoked at all for those; this null-safety is purely
 * defensive for any other non-HTTP write path.
 */
@Component
public class SecurityAuditorAware implements AuditorAware<Long> {

    @Override
    public Optional<Long> getCurrentAuditor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }
        Object principal = authentication.getPrincipal();
        return principal instanceof Long userId ? Optional.of(userId) : Optional.empty();
    }
}
