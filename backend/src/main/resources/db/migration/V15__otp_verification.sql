-- Email/mobile OTP verification at signup (see OtpCode.java / AuthService.register()).
-- Existing accounts predate this feature and are already active/usable - they're
-- grandfathered in as already verified rather than being retroactively locked out.
ALTER TABLE users ADD COLUMN email_verified BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE users ADD COLUMN mobile_verified BOOLEAN NOT NULL DEFAULT FALSE;
UPDATE users SET email_verified = TRUE, mobile_verified = TRUE;

CREATE TABLE otp_codes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    channel VARCHAR(10) NOT NULL,
    -- A hash of the code, never the raw digits - same "never store the presentable
    -- secret itself" precedent as refresh_tokens.token_hash.
    code_hash VARCHAR(255) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    consumed_at TIMESTAMP NULL,
    attempt_count INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_otp_codes_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT chk_otp_codes_channel CHECK (channel IN ('EMAIL', 'MOBILE'))
);
CREATE INDEX idx_otp_codes_user_channel ON otp_codes (user_id, channel);
