package com.clothingretail.masterdata;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SizeRepository extends JpaRepository<Size, Long> {
    List<Size> findByActiveTrueOrderByDisplayOrderAscNameAsc();

    boolean existsByNameIgnoreCase(String name);
}
