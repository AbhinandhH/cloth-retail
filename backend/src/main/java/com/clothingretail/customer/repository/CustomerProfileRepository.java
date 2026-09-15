package com.clothingretail.customer.repository;

import com.clothingretail.customer.CustomerProfile;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface CustomerProfileRepository
        extends JpaRepository<CustomerProfile, Long>, JpaSpecificationExecutor<CustomerProfile> {
    Optional<CustomerProfile> findByUserId(Long userId);
}
