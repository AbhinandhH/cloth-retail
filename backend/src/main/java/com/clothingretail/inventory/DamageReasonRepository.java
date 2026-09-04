package com.clothingretail.inventory;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DamageReasonRepository extends JpaRepository<DamageReason, Long> {
    List<DamageReason> findByActiveTrueOrderByDisplayOrderAscNameAsc();

    boolean existsByNameIgnoreCase(String name);
}
