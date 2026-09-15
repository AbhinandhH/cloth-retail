package com.clothingretail.masterdata.service;

import com.clothingretail.masterdata.dto.CategoryAdminRequest;
import com.clothingretail.masterdata.dto.CategoryAdminResponse;
import com.clothingretail.masterdata.dto.CategoryResponse;
import java.util.List;

public interface CategoryService {

    List<CategoryAdminResponse> listAdmin(String q, Boolean active);

    CategoryAdminResponse getAdmin(Long id);

    List<CategoryResponse> listPublic();

    CategoryAdminResponse create(CategoryAdminRequest request);

    CategoryAdminResponse update(Long id, CategoryAdminRequest request);

    void delete(Long id);
}
