package com.clothingretail.subscription.service;

import com.clothingretail.subscription.dto.SubscriptionPayInitiationResponse;
import com.clothingretail.subscription.dto.SubscriptionPaymentRow;
import com.clothingretail.subscription.dto.SubscriptionSettingsRequest;
import com.clothingretail.subscription.dto.SubscriptionStatusResponse;
import java.util.List;

public interface SubscriptionBillingService {

    SubscriptionStatusResponse getStatus();

    /** SUPER_ADMIN only - sets a new amount/due date and resets {@code paid} to false (a new ask means a new payment is owed). */
    SubscriptionStatusResponse updateSettings(SubscriptionSettingsRequest request);

    /** SUPER_ADMIN's manual escape hatch (offline/bank payment, or Razorpay unreachable) - independent of the gateway working. */
    SubscriptionStatusResponse markPaid();

    /** ADMIN clicks "Pay now" - creates a SubscriptionPayment row and a matching gateway order for the current amount. */
    SubscriptionPayInitiationResponse initiatePayment();

    List<SubscriptionPaymentRow> listPayments();

    /** Called by the subscription webhook once a gateway order's payment is captured - a no-op if gatewayOrderId isn't a known subscription payment (see SubscriptionWebhookController's own doc comment on why). */
    void applyPaymentOutcome(String gatewayOrderId, String gatewayPaymentId);
}
