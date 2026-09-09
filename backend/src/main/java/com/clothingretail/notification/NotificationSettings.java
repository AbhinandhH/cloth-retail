package com.clothingretail.notification;

import com.clothingretail.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Singleton row (id is always 1) holding the admin-controllable on/off switches for the OTP
 * notification channels - see AuthService, which reads this live on every register()/verifyOtp()
 * call instead of a fixed application.yml value, so an admin toggle takes effect immediately, no
 * restart needed. Created only by the V16 seed migration, same convention as SiteConfiguration.
 */
@Entity
@Table(name = "notification_settings")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class NotificationSettings extends BaseEntity {

    public static final Long SINGLETON_ID = 1L;

    @Column(name = "email_verification_enabled", nullable = false)
    private boolean emailVerificationEnabled = false;

    @Column(name = "mobile_verification_enabled", nullable = false)
    private boolean mobileVerificationEnabled = false;
}
