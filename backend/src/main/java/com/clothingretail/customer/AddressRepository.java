package com.clothingretail.customer;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AddressRepository extends JpaRepository<Address, Long> {
    List<Address> findByCustomerProfileId(Long customerProfileId);
}
