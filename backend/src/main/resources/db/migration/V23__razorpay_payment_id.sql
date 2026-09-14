-- Payment.gatewayReference stores the gateway's ORDER reference (e.g. Razorpay's "order_..." id,
-- set at payment.initiate() time, before any actual payment attempt exists). A refund, however,
-- targets a specific PAYMENT attempt (Razorpay's "pay_..." id), which only exists once the
-- gateway's webhook reports success - see PaymentWebhookService's refactored applyOutcome() and
-- RazorpayPaymentGateway.refund(). Nullable and unused by MockPaymentGateway, which has no
-- equivalent second identifier.
ALTER TABLE payments ADD COLUMN gateway_payment_id VARCHAR(100);
