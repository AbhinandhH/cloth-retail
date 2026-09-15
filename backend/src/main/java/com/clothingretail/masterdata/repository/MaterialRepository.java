package com.clothingretail.masterdata.repository;

import com.clothingretail.masterdata.Material;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MaterialRepository extends JpaRepository<Material, Long> {
    List<Material> findByActiveTrueOrderByDisplayOrderAscNameAsc();

    boolean existsByNameIgnoreCase(String name);
}
