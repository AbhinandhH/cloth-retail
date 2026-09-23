package com.clothingretail.subscription;

import com.clothingretail.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Singleton row (id is always 1) holding the software-owner's (SUPER_ADMIN) subscription terms
 * for this store: the monthly amount and the date it's due by. Not a recurring-cycle scheduler -
 * SUPER_ADMIN sets a due date directly (LocalDate, not a "day of month" rule), and picks a new one
 * whenever the next bill is due; saving a new amount/dueDate always resets {@code paid} to false,
 * since a new ask means a new payment is owed.
 *
 * <p>{@code dueDate == null} always means "never configured, never locked" - the seed migration
 * (V36) deliberately leaves it null with {@code paid = true}, so deploying this feature can never
 * itself lock an already-live site out. See {@link com.clothingretail.config.SubscriptionAccessFilter}
 * for where the lockout is actually enforced.
 */
@Entity
@Table(name = "subscription_billing")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class SubscriptionBilling extends BaseEntity {

    public static final Long SINGLETON_ID = 1L;

    @Column(name = "monthly_amount", precision = 10, scale = 2)
    private BigDecimal monthlyAmount;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(nullable = false)
    private boolean paid = true;

    @Column(name = "paid_at")
    private Instant paidAt;

    /** Razorpay payment id, or "manual" for a SUPER_ADMIN "Mark as paid" override. */
    @Column(name = "last_payment_reference", length = 100)
    private String lastPaymentReference;

    /** The core lockout predicate - see SubscriptionAccessFilter, the only caller that matters for site-wide access. */
    public boolean isLocked() {
        return dueDate != null && !paid && LocalDate.now().isAfter(dueDate);
    }
}
