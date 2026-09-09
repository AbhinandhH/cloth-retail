-- Admin-controllable on/off switches for the OTP notification channels (see
-- NotificationSettings.java / AuthService, which reads this instead of a fixed
-- application.yml value so an admin can flip these live, no restart needed).
-- Both default OFF: neither SMTP nor a real SMS provider is configured out of
-- the box, so leaving verification on by default would just break every
-- signup until an admin explicitly turns a channel on with working credentials
-- in place.
CREATE TABLE notification_settings (
    id BIGINT PRIMARY KEY,
    email_verification_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    mobile_verification_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

INSERT INTO notification_settings (id, email_verification_enabled, mobile_verification_enabled, created_at, updated_at)
VALUES (1, FALSE, FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
