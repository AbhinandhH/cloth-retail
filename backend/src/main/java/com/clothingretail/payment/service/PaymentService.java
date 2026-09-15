package com.clothingretail.payment.service;

import com.clothingretail.payment.dto.PaymentInitiateResponse;
import com.clothingretail.payment.dto.SimulatePaymentRequest;
import com.clothingretail.payment.dto.SimulatePaymentResponse;

public interface PaymentService {

    PaymentInitiateResponse initiate(Long userId, Long orderId, String paymentMethod);

    /**
     * Dev-only helper behind the customer's own JWT: looks up the payment (ownership-checked),
     * has MockPaymentGateway sign a fake webhook payload for the requested outcome, and
     * feeds it through PaymentWebhookService#handleWebhook - the exact same method a real
     * gateway's webhook call would hit.
     */
    SimulatePaymentResponse simulate(Long userId, SimulatePaymentRequest request);
}
