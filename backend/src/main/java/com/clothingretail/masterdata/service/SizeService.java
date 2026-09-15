package com.clothingretail.masterdata.service;

import com.clothingretail.masterdata.dto.SizeAdminRequest;
import com.clothingretail.masterdata.dto.SizeAdminResponse;
import com.clothingretail.masterdata.dto.SizeResponse;
import java.util.List;

public interface SizeService {

    List<SizeAdminResponse> listAdmin(String q, Boolean active);

    SizeAdminResponse getAdmin(Long id);

    List<SizeResponse> listPublic();

    SizeAdminResponse create(SizeAdminRequest request);

    SizeAdminResponse update(Long id, SizeAdminRequest request);

    void delete(Long id);
}
