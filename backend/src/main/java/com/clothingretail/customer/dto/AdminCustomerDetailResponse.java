package com.clothingretail.customer.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * The admin Customer detail view's summary panel (contact info, account status, lifetime
 * stats). Order history itself is a separate paginated endpoint (GET
 * /admin/customers/{id}/orders, reusing the existing AdminOrderRow shape) rather than embedded
 * here, so it can be paged independently of the rest of the profile.
 */
public record AdminCustomerDetailResponse(
        Long id,
        Long userId,
        String fullName,
        String email,
        String mobileNumber,
        boolean enabled,
        LocalDate dateOfBirth,
        String gender,
        long orderCount,
        BigDecimal totalSpent,
        Instant createdAt) {}
