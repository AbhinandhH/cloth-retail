package com.clothingretail.masterdata.repository;

import com.clothingretail.masterdata.SizeGroup;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SizeGroupRepository extends JpaRepository<SizeGroup, Long> {

    /**
     * Every active size group associated with the given category, ordered by the group's
     * own displayOrder - the ordering the union in
     * SizeAvailabilityService#availableSizesForCategory relies on.
     */
    @Query("SELECT DISTINCT sg FROM SizeGroup sg JOIN sg.categories c "
            + "WHERE c.id = :categoryId AND sg.active = true ORDER BY sg.displayOrder ASC, sg.name ASC")
    List<SizeGroup> findActiveByCategoryId(@Param("categoryId") Long categoryId);
}
