package com.clothingretail.masterdata;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VendorRepository extends JpaRepository<Vendor, Long> {
    List<Vendor> findByActiveTrueOrderByNameAsc();

    boolean existsByNameIgnoreCase(String name);
}
