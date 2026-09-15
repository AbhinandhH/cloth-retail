package com.clothingretail.customer.service;

import com.clothingretail.auth.User;
import com.clothingretail.auth.UserRepository;
import com.clothingretail.customer.CustomerProfile;
import com.clothingretail.customer.dto.AdminCustomerDetailResponse;
import com.clothingretail.customer.repository.CustomerProfileRepository;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@Log4j2
public class AdminCustomerServiceImpl implements AdminCustomerService {

    private final CustomerProfileRepository customerProfileRepository;
    private final UserRepository userRepository;
    private final AdminCustomerQueryService adminCustomerQueryService;

    public AdminCustomerServiceImpl(
            CustomerProfileRepository customerProfileRepository,
            UserRepository userRepository,
            AdminCustomerQueryService adminCustomerQueryService) {
        this.customerProfileRepository = customerProfileRepository;
        this.userRepository = userRepository;
        this.adminCustomerQueryService = adminCustomerQueryService;
    }

    @Override
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
