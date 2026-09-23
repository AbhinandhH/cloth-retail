package com.clothingretail.subscription;

import com.clothingretail.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * One row per subscription payment attempt (ADMIN clicks "Pay now") - append-only, same
 * "store the gateway order id up front and only trust a webhook that matches it" pattern
 * {@code payment.Payment} already uses for customer orders. This is what stops a stale/replayed
 * webhook from marking a *newer* billing cycle paid: the webhook handler only ever acts on the
 * specific row whose {@code gatewayOrderId} it's reporting on.
 */
@Entity
@Table(name = "subscription_payments")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class SubscriptionPayment extends BaseEntity {

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(name = "gateway_order_id", nullable = false, unique = true, length = 100)
    private String gatewayOrderId;

    @Column(name = "gateway_payment_id", length = 100)
    private String gatewayPaymentId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SubscriptionPaymentStatus status = SubscriptionPaymentStatus.PENDING;
}
