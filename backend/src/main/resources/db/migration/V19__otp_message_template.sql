-- Admin-configurable OTP message content (see NotificationSettings.java / OtpService, which
-- substitutes {code} and {ttlMinutes} into message_template at send time for both the email body
-- and the SMS text). email_subject is email-only. Defaults reproduce the previous hardcoded copy
-- exactly, so nothing changes for anyone who hasn't touched this yet.
ALTER TABLE notification_settings ADD COLUMN email_subject VARCHAR(255) NOT NULL DEFAULT 'Verify your email';
ALTER TABLE notification_settings ADD COLUMN message_template VARCHAR(1000) NOT NULL
    DEFAULT 'Your verification code is {code}. It expires in {ttlMinutes} minutes.';
