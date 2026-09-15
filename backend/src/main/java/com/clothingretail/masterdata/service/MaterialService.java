package com.clothingretail.masterdata.service;

import com.clothingretail.masterdata.dto.MaterialAdminRequest;
import com.clothingretail.masterdata.dto.MaterialAdminResponse;
import java.util.List;

public interface MaterialService {

    List<MaterialAdminResponse> listAdmin(String q, Boolean active);

    MaterialAdminResponse getAdmin(Long id);

    MaterialAdminResponse create(MaterialAdminRequest request);

    MaterialAdminResponse update(Long id, MaterialAdminRequest request);

    void delete(Long id);
}
