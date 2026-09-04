package com.clothingretail.inventory;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface DamageRecordRepository extends JpaRepository<DamageRecord, Long>, JpaSpecificationExecutor<DamageRecord> {
    boolean existsByProductVariantIdIn(List<Long> productVariantIds);

    long countByReasonId(Long reasonId);
}
