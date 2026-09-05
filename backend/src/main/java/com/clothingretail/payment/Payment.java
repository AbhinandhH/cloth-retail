package com.clothingretail.payment;

import com.clothingretail.common.BaseEntity;
import com.clothingretail.order.Order;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "payments")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class Payment extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status;

    @Column(name = "gateway_reference", nullable = false, unique = true, length = 100)
    private String gatewayReference;

    /** Set only once the payment is resolved by a webhook - null while still PENDING. Unique so a replayed/duplicate webhook event can never be applied twice. */
    @Column(name = "webhook_event_id", unique = true, length = 100)
    private String webhookEventId;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    /** Customer-supplied at initiation time, defaulting to "UPI" when omitted - see PaymentService.initiate. */
    @Column(name = "payment_method", length = 30)
    private String paymentMethod;
}
