package com.clothingretail.inventory.service;

import com.clothingretail.inventory.dto.DamageReasonAdminRequest;
import com.clothingretail.inventory.dto.DamageReasonAdminResponse;
import com.clothingretail.inventory.dto.DamageReasonResponse;
import java.util.List;

public interface DamageReasonService {

    List<DamageReasonAdminResponse> listAdmin(String q, Boolean active);

    DamageReasonAdminResponse getAdmin(Long id);

    List<DamageReasonResponse> listPublic();

    DamageReasonAdminResponse create(DamageReasonAdminRequest request);

    DamageReasonAdminResponse update(Long id, DamageReasonAdminRequest request);

    void delete(Long id);
}
