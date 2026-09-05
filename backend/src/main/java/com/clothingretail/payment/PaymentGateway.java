package com.clothingretail.payment;

import com.clothingretail.order.Order;
import java.math.BigDecimal;

/**
 * Boundary to whatever actually moves money. {@link #verifySignature} is the security boundary
 * for the webhook endpoint (which trusts a signature instead of a customer JWT - see
 * PaymentController), so any real implementation must make it airtight.
 */
public interface PaymentGateway {

    /** Starts a payment attempt for {@code order} and returns a reference to look it up by later. */
    PaymentInitiation initiate(Order order);

    /** Verifies that {@code signature} is a valid signature of {@code payload}, produced by this gateway. */
    boolean verifySignature(String payload, String signature);

    /** Starts a refund of {@code amount} against a previously-successful {@code payment}. */
    RefundInitiation refund(Payment payment, BigDecimal amount);
}
