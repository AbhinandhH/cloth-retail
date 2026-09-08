package com.clothingretail.customer;

import com.clothingretail.auth.User;
import com.clothingretail.auth.UserRepository;
import com.clothingretail.customer.dto.AdminCustomerDetailResponse;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Write-side of the admin Customers module: just the account-status toggle. There's no separate
 * "suspended" state in this schema (see User.enabled's own doc comment) - disabling here reuses
 * the exact same flag AuthService.login already checks, so this genuinely blocks the customer
 * from signing in, not just a cosmetic label.
 */
@Service
@Transactional
@Log4j2
public class AdminCustomerService {

    private final CustomerProfileRepository customerProfileRepository;
    private final UserRepository userRepository;
    private final AdminCustomerQueryService adminCustomerQueryService;

    public AdminCustomerService(
            CustomerProfileRepository customerProfileRepository,
            UserRepository userRepository,
            AdminCustomerQueryService adminCustomerQueryService) {
        this.customerProfileRepository = customerProfileRepository;
        this.userRepository = userRepository;
        this.adminCustomerQueryService = adminCustomerQueryService;
    }

    public AdminCustomerDetailResponse setEnabled(Long customerProfileId, boolean enabled) {
        log.info("[1508] Setting customer account status customerProfileId={} enabled={}", customerProfileId, enabled);
        CustomerProfile profile = adminCustomerQueryService.find(customerProfileId);
        User user = profile.getUser();
        user.setEnabled(enabled);
        userRepository.save(user);
        log.info("[1509] Customer account status updated customerProfileId={} userId={} enabled={}", customerProfileId, user.getId(), enabled);
        return adminCustomerQueryService.detail(customerProfileId);
    }
}
