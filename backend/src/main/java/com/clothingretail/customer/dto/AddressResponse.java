package com.clothingretail.customer.dto;

public record AddressResponse(
        Long id,
        String label,
        String addressLine1,
        String addressLine2,
        String city,
        String state,
        String postalCode,
        String country,
        boolean isDefault) {}
