package com.clothingretail.siteconfig.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Body for {@code PUT /api/admin/configuration}. activeTheme is deliberately excluded - it only changes via POST /api/admin/themes/{id}/activate. */
public record SiteConfigurationUpdateRequest(
        @NotBlank(message = "must not be blank") String businessName,
        String tagline,
        String logoUrl,
        String faviconUrl,
        String contactEmail,
        String contactPhone,
        String instagramUrl,
        String whatsappNumber,
        String facebookUrl,
        String footerText,
        String loginBackgroundImageUrl,
        String loginPromoImageUrl,
        String loginPromoText,
        String registrationImageUrl,
        @NotNull(message = "must not be null") @Min(value = 1, message = "must be at least 1 minute")
                @Max(value = 1440, message = "must not exceed 1440 minutes (24 hours)") Integer orderReservationTtlMinutes,
        @NotNull(message = "must not be null") Boolean reserveStockOnlyAtPayment,
        @NotNull(message = "must not be null") @Min(value = 1, message = "must be at least 1 minutes")
                @Max(value = 1440, message = "must not exceed 1440 minutes (24 hours)") Integer idleTimeoutMinutes) {}
