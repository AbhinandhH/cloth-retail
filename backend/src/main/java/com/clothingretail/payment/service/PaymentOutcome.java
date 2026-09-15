package com.clothingretail.payment.service;

/** The outcome a gateway webhook (real or simulated) reports for a payment attempt. */
public enum PaymentOutcome {
    SUCCESS,
    FAILURE
}
