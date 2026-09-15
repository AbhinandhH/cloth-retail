package com.clothingretail.customer.service;

import com.clothingretail.customer.dto.AdminCustomerDetailResponse;

/**
 * Write-side of the admin Customers module: just the account-status toggle. There's no separate
 * "suspended" state in this schema (see User.enabled's own doc comment) - disabling here reuses
 * the exact same flag AuthService.login already checks, so this genuinely blocks the customer
 * from signing in, not just a cosmetic label.
 */
public interface AdminCustomerService {

    AdminCustomerDetailResponse setEnabled(Long customerProfileId, boolean enabled);
}
