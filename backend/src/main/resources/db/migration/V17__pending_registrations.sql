-- A customer row must never exist in `users` until every required OTP channel is confirmed - an
-- abandoned/never-verified signup attempt should leave no trace of a real account. Registration
-- data (name, email, mobile, hashed password) is held here instead, and only promoted into
-- `users`/`customer_profiles` by AuthService.verifyOtp() once verification is fully complete - see
-- AuthService.register(), which no longer touches `users` at all for the OTP-required path.
CREATE TABLE pending_registrations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    full_name VARCHAR(150) NOT NULL,
    email VARCHAR(255) NOT NULL,
    mobile_number VARCHAR(20),
    password_hash VARCHAR(255) NOT NULL,
    email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    mobile_verified BOOLEAN NOT NULL DEFAULT FALSE,
    -- Abandoned attempts (never completed) are reclaimed by PendingRegistrationCleanupJob rather
    -- than accumulating forever - generous enough that a legitimate resend/retry over a day isn't
    -- affected.
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT uq_pending_registrations_email UNIQUE (email)
);

-- Every OTP is for an in-progress signup, never for an already-existing account now - repoint
-- otp_codes at pending_registrations instead of users. Rows are cleared first (OTP codes are
-- short-lived by design; nothing durable is lost) rather than assumed empty, so this doesn't
-- depend on exactly when it happens to run relative to other cleanup.
DELETE FROM otp_codes;
-- The FK constraint must be dropped before the index it depends on - MySQL refuses to drop an
-- index that's still backing a foreign key.
ALTER TABLE otp_codes DROP FOREIGN KEY fk_otp_codes_user;
DROP INDEX idx_otp_codes_user_channel ON otp_codes;
ALTER TABLE otp_codes DROP COLUMN user_id;
ALTER TABLE otp_codes ADD COLUMN pending_registration_id BIGINT NOT NULL;
ALTER TABLE otp_codes ADD CONSTRAINT fk_otp_codes_pending_registration
    FOREIGN KEY (pending_registration_id) REFERENCES pending_registrations (id) ON DELETE CASCADE;
CREATE INDEX idx_otp_codes_pending_registration_channel ON otp_codes (pending_registration_id, channel);

-- A `users` row is only ever created once fully verified, so these columns would always read
-- true there from now on - dead weight. The equivalent flags now live on pending_registrations
-- above, which is the only place they're ever actually false.
ALTER TABLE users DROP COLUMN email_verified;
ALTER TABLE users DROP COLUMN mobile_verified;
