package com.clothingretail.masterdata.dto;

public record SubCategoryAdminResponse(
        Long id, String name, String slug, Long categoryId, String categoryName, int displayOrder, boolean active) {}
