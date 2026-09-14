package com.clothingretail.payment.dto;

/**
 * Public, unauthenticated - tells the frontend which checkout UI to render and, for Razorpay,
 * the public key id it needs to open Checkout.js. {@code keyId} is the publishable half of the
 * credential pair (safe to expose to a browser) - never the key secret or webhook secret.
 */
public record PaymentConfigResponse(String provider, String keyId) {}
