-- Nullable back-reference from an order line to the cart_items row it was created from - see
-- OrderItem.sourceCartItemId's own doc comment. Order creation no longer clears the cart
-- immediately (OrderCreationService): a PENDING_PAYMENT order isn't a guaranteed sale yet, so the
-- cart line has to survive a failed/expired payment untouched. The matching cart line is instead
-- removed only once payment actually succeeds (PaymentWebhookServiceImpl#applyOutcome), which
-- needs this column to know exactly which cart_items row to delete. No FK constraint: the
-- referenced row is expected to be deleted by that same success-time cleanup, and may already be
-- gone if the customer removed it manually before payment completed - it's a best-effort soft
-- reference, same pattern as inventory_transactions.reference_id.
ALTER TABLE order_items ADD COLUMN source_cart_item_id BIGINT NULL;
