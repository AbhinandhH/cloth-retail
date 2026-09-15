package com.clothingretail.returns.repository;

import com.clothingretail.auth.User;
import com.clothingretail.customer.CustomerProfile;
import com.clothingretail.order.Order;
import com.clothingretail.order.OrderItem;
import com.clothingretail.returns.RequestType;
import com.clothingretail.returns.ReturnRequest;
import com.clothingretail.returns.ReturnRequestStatus;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

/** Builds filter predicates for the admin return/exchange list - mirrors OrderSpecifications' style. */
public final class ReturnRequestSpecifications {

    private ReturnRequestSpecifications() {}

    public static Specification<ReturnRequest> filter(RequestType requestType, ReturnRequestStatus status, String q) {
        return (root, query, cb) -> {
            if (!isCountQuery(query)) {
                root.fetch("order", JoinType.INNER);
                root.fetch("orderItem", JoinType.INNER);
                root.fetch("customerProfile", JoinType.INNER).fetch("user", JoinType.INNER);
            }
            Join<ReturnRequest, Order> orderJoin = root.join("order", JoinType.INNER);
            Join<ReturnRequest, OrderItem> itemJoin = root.join("orderItem", JoinType.INNER);
            Join<ReturnRequest, CustomerProfile> profileJoin = root.join("customerProfile", JoinType.INNER);
            Join<CustomerProfile, User> userJoin = profileJoin.join("user", JoinType.INNER);

            List<Predicate> predicates = new ArrayList<>();
            if (requestType != null) {
                predicates.add(cb.equal(root.get("requestType"), requestType));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (q != null && !q.isBlank()) {
                String pattern = "%" + q.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(orderJoin.get("orderNumber")), pattern),
                        cb.like(cb.lower(itemJoin.get("productName")), pattern),
                        cb.like(cb.lower(itemJoin.get("sku")), pattern),
                        cb.like(cb.lower(userJoin.get("fullName")), pattern),
                        cb.like(cb.lower(userJoin.get("email")), pattern)));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private static boolean isCountQuery(CriteriaQuery<?> query) {
        Class<?> resultType = query.getResultType();
        return resultType == Long.class || resultType == long.class;
    }
}
