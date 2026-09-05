package com.clothingretail.order.dto;

import java.time.LocalDate;

/** Every field is nullable - PUT /{id}/shipment upserts (creates if absent, updates if present). */
public record AdminShipmentRequest(
        String provider, String trackingNumber, LocalDate shipmentDate, LocalDate deliveryDate, String notes) {}
