package com.clothingretail.masterdata.dto;

public record CategoryAdminResponse(Long id, String name, String slug, int displayOrder, boolean active) {}
