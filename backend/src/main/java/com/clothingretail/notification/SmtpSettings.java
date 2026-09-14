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
 * Singleton row (id is always 1) holding admin-configurable email credentials, for either of two
 * providers (see {@link #provider}) - see EmailSenderImpl, which builds a fresh sender from this
 * on every send rather than relying on Spring Boot's spring.mail.* auto-configuration, so an
 * admin change here takes effect on the very next email, no restart needed. Created only by the
 * V18 seed migration, same convention as NotificationSettings/SiteConfiguration.
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

    /** "SMTP" (default) or "RESEND" - see EmailSenderImpl, which dispatches on this. */
    @Column(nullable = false, length = 20)
    private String provider = "SMTP";

    /** Only used when provider is "RESEND" - same plaintext-storage posture as password above. */
    @Column(name = "api_key", length = 255)
    private String apiKey;

    public boolean isConfigured() {
        return "RESEND".equals(provider)
                ? apiKey != null && !apiKey.isBlank()
                : host != null && !host.isBlank();
    }
}
