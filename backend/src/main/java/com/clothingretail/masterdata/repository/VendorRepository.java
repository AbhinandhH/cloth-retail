package com.clothingretail.masterdata.repository;

import com.clothingretail.masterdata.Vendor;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VendorRepository extends JpaRepository<Vendor, Long> {
    List<Vendor> findByActiveTrueOrderByNameAsc();

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    boolean existsByContactEmailIgnoreCase(String contactEmail);

    boolean existsByContactEmailIgnoreCaseAndIdNot(String contactEmail, Long id);

    boolean existsByContactPhone(String contactPhone);

    boolean existsByContactPhoneAndIdNot(String contactPhone, Long id);
}
