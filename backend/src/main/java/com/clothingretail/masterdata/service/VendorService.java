package com.clothingretail.masterdata.service;

import com.clothingretail.masterdata.dto.VendorAdminRequest;
import com.clothingretail.masterdata.dto.VendorAdminResponse;
import com.clothingretail.masterdata.dto.VendorResponse;
import java.util.List;

public interface VendorService {

    List<VendorAdminResponse> listAdmin(String q, Boolean active);

    VendorAdminResponse getAdmin(Long id);

    List<VendorResponse> listPublic();

    VendorAdminResponse create(VendorAdminRequest request);

    VendorAdminResponse update(Long id, VendorAdminRequest request);

    void delete(Long id);
}
