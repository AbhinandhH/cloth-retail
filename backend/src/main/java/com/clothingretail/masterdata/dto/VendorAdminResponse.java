package com.clothingretail.masterdata.dto;

public record VendorAdminResponse(
        Long id, String name, String contactName, String contactEmail, String contactPhone, boolean active) {}
