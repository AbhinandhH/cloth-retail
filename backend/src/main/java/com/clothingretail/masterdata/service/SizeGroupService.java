package com.clothingretail.masterdata.service;

import com.clothingretail.masterdata.dto.SizeGroupAdminRequest;
import com.clothingretail.masterdata.dto.SizeGroupAdminResponse;
import java.util.List;

/**
 * Owns SizeGroup's association-management logic (which categories/sizes belong to a group,
 * and each size's position within it) on top of plain CRUD. A group has no delete-guard: it
 * only ever feeds the "which sizes can I pick from for this category" computation (see
 * SizeAvailabilityService) - ProductVariant.size still points directly at the flat global
 * Size table, so removing a group can never orphan anything.
 */
public interface SizeGroupService {

    List<SizeGroupAdminResponse> listAdmin(String q, Boolean active);

    SizeGroupAdminResponse getAdmin(Long id);

    SizeGroupAdminResponse create(SizeGroupAdminRequest request);

    SizeGroupAdminResponse update(Long id, SizeGroupAdminRequest request);

    void delete(Long id);
}
