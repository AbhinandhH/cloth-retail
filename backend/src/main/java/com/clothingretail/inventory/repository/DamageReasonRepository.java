package com.clothingretail.inventory.repository;

import com.clothingretail.inventory.DamageReason;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DamageReasonRepository extends JpaRepository<DamageReason, Long> {
    List<DamageReason> findByActiveTrueOrderByDisplayOrderAscNameAsc();

    boolean existsByNameIgnoreCase(String name);
}
