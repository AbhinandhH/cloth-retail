package com.clothingretail.customer.dto;

import java.math.BigDecimal;
import java.time.Instant;

/** One row of the admin Customers list - see AdminCustomerQueryService. */
public record AdminCustomerRow(
        Long id,
        Long userId,
        String fullName,
        String email,
        String mobileNumber,
        boolean enabled,
        long orderCount,
        BigDecimal totalSpent,
        Instant createdAt) {}
