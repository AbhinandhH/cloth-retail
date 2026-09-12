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
 * Singleton row (id is always 1) holding admin-configurable SMTP credentials - see
 * SmtpEmailSender, which builds a fresh JavaMailSenderImpl from this on every send rather than
 * relying on Spring Boot's spring.mail.* auto-configuration, so an admin change here takes effect
 * on the very next email, no restart needed. Created only by the V18 seed migration, same
 * convention as NotificationSettings/SiteConfiguration.
 */
@Entity
@Table(name = "smtp_settings")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class SmtpSettings extends BaseEntity {

    public static final Long SINGLETON_ID = 1L;

    @Column(length = 255)
    private String host;

    @Column(nullable = false)
    private int port = 587;

    @Column(length = 255)
    private String username;

    @Column(length = 255)
    private String password;

    @Column(name = "from_address", length = 255)
    private String fromAddress;

    @Column(name = "use_starttls", nullable = false)
    private boolean useStarttls = true;

    public boolean isConfigured() {
        return host != null && !host.isBlank();
    }
}
