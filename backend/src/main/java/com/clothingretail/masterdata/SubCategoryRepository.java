package com.clothingretail.masterdata;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubCategoryRepository extends JpaRepository<SubCategory, Long> {
    List<SubCategory> findByActiveTrueOrderByDisplayOrderAscNameAsc();

    List<SubCategory> findByCategoryIdAndActiveTrueOrderByDisplayOrderAscNameAsc(Long categoryId);

    boolean existsBySlugIgnoreCase(String slug);

    long countByCategoryId(Long categoryId);
}
