package com.clothingretail.customer;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AddressRepository extends JpaRepository<Address, Long> {
    List<Address> findByCustomerProfileId(Long customerProfileId);

    Optional<Address> findByIdAndCustomerProfileId(Long id, Long customerProfileId);

    /** Unsets isDefault on every address for this profile - used before promoting a new default. */
    @Modifying
    @Query("UPDATE Address a SET a.isDefault = false WHERE a.customerProfile.id = :profileId")
    void clearDefault(@Param("profileId") Long profileId);
}
