package com.clothingretail.payment.dto;

/**
 * Public, unauthenticated - tells the frontend which checkout UI to render and, for Razorpay,
 * the public key id it needs to open Checkout.js. {@code keyId} is the publishable half of the
 * credential pair (safe to expose to a browser) - never the key secret or webhook secret.
 *
 * {@code reserveStockOnlyAtPayment} mirrors {@code SiteConfiguration.reserveStockOnlyAtPayment} -
 * when true, the Payment page must NOT auto-initiate payment on mount (that would defeat the
 * whole point by reserving stock the instant the page loads), and instead waits for the
 * customer's explicit "Pay" click.
 */
public record PaymentConfigResponse(String provider, String keyId, boolean reserveStockOnlyAtPayment) {}
