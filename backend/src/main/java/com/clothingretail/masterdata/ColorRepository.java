package com.clothingretail.masterdata;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ColorRepository extends JpaRepository<Color, Long> {
    List<Color> findByActiveTrueOrderByDisplayOrderAscNameAsc();

    boolean existsByNameIgnoreCase(String name);
}
