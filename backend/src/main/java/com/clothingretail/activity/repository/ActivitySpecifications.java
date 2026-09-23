package com.clothingretail.activity.repository;

import com.clothingretail.activity.ActivityLog;
import jakarta.persistence.criteria.Predicate;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

/** Builds filter predicates for the admin activity log listing, mirroring {@code InventorySpecifications}' style. */
public final class ActivitySpecifications {

    private ActivitySpecifications() {}

    public static Specification<ActivityLog> filter(
            Long actorId, String module, String httpMethod, Instant dateFrom, Instant dateTo) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (actorId != null) {
                predicates.add(cb.equal(root.get("actorId"), actorId));
            }
            if (StringUtils.hasText(module)) {
                predicates.add(cb.equal(root.get("module"), module));
            }
            if (StringUtils.hasText(httpMethod)) {
                predicates.add(cb.equal(root.get("httpMethod"), httpMethod));
            }
            if (dateFrom != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), dateFrom));
            }
            if (dateTo != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), dateTo));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
