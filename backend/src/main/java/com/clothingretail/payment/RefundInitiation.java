package com.clothingretail.payment;

/** What a {@link PaymentGateway} hands back after starting a refund attempt - same shape as {@link PaymentInitiation}. */
public record RefundInitiation(String reference) {}
