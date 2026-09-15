package com.clothingretail.common;

import com.clothingretail.auth.repository.UserRepository;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * Resolves the {@code Long} user ids stored in {@link AuditableMasterEntity#getCreatedBy()}/
 * {@code getUpdatedBy()} to the acting user's {@code fullName}, for admin response DTOs
 * ("created by" / "updated by" columns). Batches lookups so mapping a whole list only
 * costs one extra query instead of two per row.
 */
@Component
public class AuditorNameResolver {

    private final UserRepository userRepository;

    public AuditorNameResolver(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /** Convenience for a single entity - prefer {@link #resolveNames(Collection)} when mapping a list. */
    public String resolve(Long userId) {
        if (userId == null) {
            return null;
        }
        return userRepository.findById(userId).map(u -> u.getFullName()).orElse(null);
    }

    /**
     * Convenience for the create/update/getAdmin single-entity response path:
     * {@code resolveNames(entity.getCreatedBy(), entity.getUpdatedBy())}. Deliberately NOT
     * {@code resolveNames(List.of(createdBy, updatedBy))} at call sites - {@link List#of}
     * throws NPE immediately if either argument is {@code null}, which both very commonly
     * are (every migration-seeded row has no acting user; an update on one leaves createdBy
     * null while updatedBy becomes non-null).
     */
    public Map<Long, String> resolveNames(Long createdBy, Long updatedBy) {
        return resolveNames(java.util.Arrays.asList(createdBy, updatedBy));
    }

    /**
     * Callers do {@code names.get(entity.getCreatedBy())} where getCreatedBy() is very
     * commonly {@code null} (every row seeded by a migration, before this master-data pass,
     * has no acting user at all) - so this MUST tolerate a {@code null} key on lookup.
     * {@link Map#of()} (used for the empty case) throws NPE on {@code get(null)}, unlike a
     * plain {@link HashMap} (used for the non-empty case, via {@link Collectors#toMap}) or
     * {@link Collections#emptyMap()}, both of which return {@code null} instead.
     */
    public Map<Long, String> resolveNames(Collection<Long> userIds) {
        Set<Long> ids = new HashSet<>();
        for (Long id : userIds) {
            if (id != null) {
                ids.add(id);
            }
        }
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        return userRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(u -> u.getId(), u -> u.getFullName()));
    }
}
