package com.clothingretail.siteconfig;

import com.clothingretail.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Singleton row (id is always 1) holding storefront branding, contact info,
 * and login-page visuals. Created only by the V4 seed migration - application
 * code only ever reads/updates the existing row, never creates one. If it's
 * ever missing at id=1, that's a startup-time misconfiguration to fix in the
 * database, not something for application code to silently recover from.
 */
@Entity
@Table(name = "site_configuration")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class SiteConfiguration extends BaseEntity {

    public static final Long SINGLETON_ID = 1L;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "active_theme_id", nullable = false)
    private Theme activeTheme;

    @Column(name = "business_name", nullable = false, length = 150)
    private String businessName;

    @Column(length = 255)
    private String tagline;

    @Column(name = "logo_url", length = 500)
    private String logoUrl;

    @Column(name = "favicon_url", length = 500)
    private String faviconUrl;

    @Column(name = "contact_email", length = 255)
    private String contactEmail;

    @Column(name = "contact_phone", length = 30)
    private String contactPhone;

    @Column(name = "instagram_url", length = 500)
    private String instagramUrl;

    @Column(name = "whatsapp_number", length = 30)
    private String whatsappNumber;

    @Column(name = "facebook_url", length = 500)
    private String facebookUrl;

    @Column(name = "footer_text", length = 500)
    private String footerText;

    @Column(name = "login_background_image_url", length = 500)
    private String loginBackgroundImageUrl;

    @Column(name = "login_promo_image_url", length = 500)
    private String loginPromoImageUrl;

    @Column(name = "login_promo_text", length = 500)
    private String loginPromoText;

    @Column(name = "registration_image_url", length = 500)
    private String registrationImageUrl;
}
