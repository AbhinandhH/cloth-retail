package com.clothingretail.subscription.dto;

import java.math.BigDecimal;

/** keyId is the publishable half of the Razorpay credential pair - safe to expose to a browser, same as PaymentConfigResponse.keyId. */
public record SubscriptionPayInitiationResponse(String gatewayOrderId, String keyId, BigDecimal amount) {}
