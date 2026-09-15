package com.clothingretail.customer.service;

import com.clothingretail.customer.CustomerProfile;
import com.clothingretail.customer.dto.AdminCustomerDetailResponse;
import com.clothingretail.customer.dto.AdminCustomerRow;
import com.clothingretail.order.dto.AdminOrderRow;
import org.springframework.data.domain.Page;

/**
 * Read-side of the admin Customers module: the paginated/searchable list and the single-customer
 * detail view (contact info, account status, lifetime order stats, order history) - kept
 * separate from {@link AdminCustomerService} (the write side, just the enable/disable toggle),
 * same split as the order package's query/write services.
 */
public interface AdminCustomerQueryService {

    Page<AdminCustomerRow> list(String q, Boolean enabled, String sort, String dir, int page, int size);

    AdminCustomerDetailResponse detail(Long id);

    Page<AdminOrderRow> orderHistory(Long id, int page, int size);

    /** Used by {@link AdminCustomerService} too - not just this service's own list/detail. */
    CustomerProfile find(Long id);
}
