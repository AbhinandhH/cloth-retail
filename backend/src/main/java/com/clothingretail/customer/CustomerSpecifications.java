package com.clothingretail.customer;

import com.clothingretail.auth.User;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

/**
 * Builds filter predicates for the admin Customers list - mirrors {@code OrderSpecifications}'
 * style exactly (same fetch-vs-count-query guard, same "search across name/email/phone" OR
 * pattern). {@code enabled} filters on account status (see User.enabled - there's no separate
 * status enum in this schema, just this one boolean).
 */
public final class CustomerSpecifications {

    private CustomerSpecifications() {}

    public static Specification<CustomerProfile> filter(String q, Boolean enabled) {
        return (root, query, cb) -> {
            if (!isCountQuery(query)) {
                root.fetch("user", JoinType.INNER);
            }
            Join<CustomerProfile, User> userJoin = root.join("user", JoinType.INNER);

            List<Predicate> predicates = new ArrayList<>();
            if (enabled != null) {
                predicates.add(cb.equal(userJoin.get("enabled"), enabled));
            }
            if (q != null && !q.isBlank()) {
                String pattern = "%" + q.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(userJoin.get("fullName")), pattern),
                        cb.like(cb.lower(userJoin.get("email")), pattern),
                        cb.like(cb.lower(cb.coalesce(userJoin.get("mobileNumber"), "")), pattern)));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private static boolean isCountQuery(CriteriaQuery<?> query) {
        Class<?> resultType = query.getResultType();
        return resultType == Long.class || resultType == long.class;
    }
}
