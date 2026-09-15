package com.clothingretail.order.repository;

import com.clothingretail.auth.User;
import com.clothingretail.customer.CustomerProfile;
import com.clothingretail.order.Order;
import com.clothingretail.order.OrderStatus;
import com.clothingretail.payment.Payment;
import com.clothingretail.payment.PaymentStatus;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

/**
 * Builds filter predicates for the admin order list/dashboard - mirrors {@code
 * InventorySpecifications}' style. Also eager-fetches {@code customerProfile -> user} on the
 * content query (never the count query - a fetch on a query returning {@code Long} is invalid)
 * so {@code AdminOrderQueryService} can read the customer's name/contact off each row without a
 * lazy load per order, which would be N+1.
 */
public final class OrderSpecifications {

    private OrderSpecifications() {}

    public static Specification<Order> filter(
            String q,
            OrderStatus orderStatus,
            PaymentStatus paymentStatus,
            Instant dateFrom,
            Instant dateTo,
            String paymentMethod) {
        return (root, query, cb) -> {
            if (!isCountQuery(query)) {
                root.fetch("customerProfile", JoinType.INNER).fetch("user", JoinType.INNER);
            }
            Join<Order, CustomerProfile> customerProfileJoin = root.join("customerProfile", JoinType.INNER);
            Join<CustomerProfile, User> userJoin = customerProfileJoin.join("user", JoinType.INNER);

            List<Predicate> predicates = new ArrayList<>();

            if (orderStatus != null) {
                predicates.add(cb.equal(root.get("status"), orderStatus));
            }
            if (dateFrom != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), dateFrom));
            }
            if (dateTo != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), dateTo));
            }
            if (q != null && !q.isBlank()) {
                String pattern = "%" + q.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("orderNumber")), pattern),
                        cb.like(cb.lower(userJoin.get("fullName")), pattern),
                        cb.like(cb.lower(cb.coalesce(userJoin.get("mobileNumber"), "")), pattern),
                        cb.like(cb.lower(userJoin.get("email")), pattern)));
            }
            if (paymentStatus != null || paymentMethod != null) {
                predicates.add(cb.exists(latestPaymentSubquery(root, query, cb, paymentStatus, paymentMethod)));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    /** Correlated EXISTS subquery matching this order's most recent payment attempt against the requested status/method. */
    private static Subquery<Long> latestPaymentSubquery(
            Root<Order> order, CriteriaQuery<?> query, CriteriaBuilder cb, PaymentStatus paymentStatus, String paymentMethod) {
        Subquery<Long> sub = query.subquery(Long.class);
        Root<Payment> payment = sub.from(Payment.class);

        Subquery<Long> latestIdSub = sub.subquery(Long.class);
        Root<Payment> latestPaymentRoot = latestIdSub.from(Payment.class);
        latestIdSub.select(cb.max(latestPaymentRoot.get("id")));
        latestIdSub.where(cb.equal(latestPaymentRoot.get("order"), order));

        List<Predicate> predicates = new ArrayList<>();
        predicates.add(cb.equal(payment.get("order"), order));
        predicates.add(cb.equal(payment.get("id"), latestIdSub));
        if (paymentStatus != null) {
            predicates.add(cb.equal(payment.get("status"), paymentStatus));
        }
        if (paymentMethod != null) {
            predicates.add(cb.equal(payment.get("paymentMethod"), paymentMethod));
        }

        sub.select(payment.get("id"));
        sub.where(predicates.toArray(new Predicate[0]));
        return sub;
    }

    private static boolean isCountQuery(CriteriaQuery<?> query) {
        Class<?> resultType = query.getResultType();
        return resultType == Long.class || resultType == long.class;
    }
}
