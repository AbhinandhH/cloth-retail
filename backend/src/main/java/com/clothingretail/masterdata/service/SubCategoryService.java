package com.clothingretail.masterdata.service;

import com.clothingretail.masterdata.dto.SubCategoryAdminRequest;
import com.clothingretail.masterdata.dto.SubCategoryAdminResponse;
import com.clothingretail.masterdata.dto.SubCategoryResponse;
import java.util.List;

public interface SubCategoryService {

    List<SubCategoryAdminResponse> listAdmin(String q, Boolean active);

    SubCategoryAdminResponse getAdmin(Long id);

    List<SubCategoryResponse> listPublic(Long categoryId);

    SubCategoryAdminResponse create(SubCategoryAdminRequest request);

    SubCategoryAdminResponse update(Long id, SubCategoryAdminRequest request);

    void delete(Long id);
}
