package com.clothingretail.masterdata.repository;

import com.clothingretail.masterdata.SizeChart;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SizeChartRepository extends JpaRepository<SizeChart, Long> {

    boolean existsByNameIgnoreCase(String name);
}
