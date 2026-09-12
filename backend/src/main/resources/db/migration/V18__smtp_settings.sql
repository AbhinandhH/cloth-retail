-- Admin-configurable SMTP credentials (see SmtpSettings.java / SmtpEmailSender), replacing the
-- old spring.mail.*/env-var-only setup - an admin can now set/change these live from the
-- Notifications module, no server restart or .yml edit needed. Singleton row, same convention as
-- notification_settings/site_configuration.
CREATE TABLE smtp_settings (
    id BIGINT PRIMARY KEY,
    host VARCHAR(255),
    port INT NOT NULL DEFAULT 587,
    username VARCHAR(255),
    -- Plain text, same security posture as the rest of this dev app's data (no at-rest
    -- encryption anywhere else either) - never returned by the admin GET endpoint though, only
    -- ever written to, see SmtpSettingsResponse.
    password VARCHAR(255),
    from_address VARCHAR(255),
    use_starttls BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

INSERT INTO smtp_settings (id, port, use_starttls, created_at, updated_at)
VALUES (1, 587, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
