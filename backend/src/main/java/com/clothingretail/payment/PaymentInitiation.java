package com.clothingretail.payment;

/** What a {@link PaymentGateway} hands back after starting a payment attempt. */
public record PaymentInitiation(String gatewayReference) {}
