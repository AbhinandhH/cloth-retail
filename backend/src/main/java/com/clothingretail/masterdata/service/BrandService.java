package com.clothingretail.masterdata.service;

import com.clothingretail.masterdata.dto.BrandAdminRequest;
import com.clothingretail.masterdata.dto.BrandAdminResponse;
import java.util.List;

public interface BrandService {

    List<BrandAdminResponse> listAdmin(String q, Boolean active);

    BrandAdminResponse getAdmin(Long id);

    BrandAdminResponse create(BrandAdminRequest request);

    BrandAdminResponse update(Long id, BrandAdminRequest request);

    void delete(Long id);
}
