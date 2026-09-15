package com.clothingretail.tax;

import com.clothingretail.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Singleton row (id is always 1) holding the storefront's GST rates - see OrderCreationService,
 * which reads this to compute cgstAmount/sgstAmount on every new order. Deliberately just the two
 * rates admins asked for (no IGST/inter-state handling) - both apply uniformly to every order's
 * post-discount goods total, not per-product or per-category. Created only by the V24 seed
 * migration, same convention as NotificationSettings/SmtpSettings/SiteConfiguration.
 */
@Entity
@Table(name = "tax_settings")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class TaxSettings extends BaseEntity {

    public static final Long SINGLETON_ID = 1L;

    @Column(name = "cgst_percent", nullable = false, precision = 5, scale = 2)
    private BigDecimal cgstPercent = BigDecimal.ZERO;

    @Column(name = "sgst_percent", nullable = false, precision = 5, scale = 2)
    private BigDecimal sgstPercent = BigDecimal.ZERO;
}
