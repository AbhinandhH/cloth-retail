package com.clothingretail.order.dto;

import java.time.LocalDate;

/** Used both as the order detail's {@code shipment} field and as the PUT /{id}/shipment response body. */
public record AdminShipmentResponse(
        String provider, String trackingNumber, LocalDate shipmentDate, LocalDate deliveryDate, String notes) {}
