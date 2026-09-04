package com.clothingretail.order.dto;

public record OrderShippingAddressResponse(
        String addressLine1,
        String addressLine2,
        String city,
        String state,
        String postalCode,
        String country) {}
