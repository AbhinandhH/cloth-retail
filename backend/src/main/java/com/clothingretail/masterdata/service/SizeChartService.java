package com.clothingretail.masterdata.service;

import com.clothingretail.masterdata.dto.SizeChartAdminRequest;
import com.clothingretail.masterdata.dto.SizeChartAdminResponse;
import java.util.List;

/**
 * Owns SizeChart CRUD plus its columns/rows association management. Unlike SizeGroup, a
 * SizeChart DOES have a delete-guard: Product.sizeChart is a real FK (see
 * ProductRepository#countBySizeChartId), so removing an in-use chart would either orphan
 * products or need a cascading nullify - deactivating instead is the supported path, same as
 * Brand/Material.
 */
public interface SizeChartService {

    List<SizeChartAdminResponse> listAdmin(String q, Boolean active);

    SizeChartAdminResponse getAdmin(Long id);

    SizeChartAdminResponse create(SizeChartAdminRequest request);

    SizeChartAdminResponse update(Long id, SizeChartAdminRequest request);

    void delete(Long id);
}
