package com.clothingretail.auth.service;

import com.clothingretail.auth.OtpChannel;
import com.clothingretail.auth.PendingRegistration;

/**
 * Generates, sends, and verifies OTP codes for email/mobile verification at signup. Codes are
 * never stored in plaintext - only their SHA-256 hash, reusing JwtService#hashToken since it's a
 * generic string hash, not JWT-specific.
 */
public interface OtpService {

    /** Invalidates any outstanding code for this (pending registration, channel), generates a fresh one, and sends it. */
    void generateAndSend(PendingRegistration registration, OtpChannel channel);

    /** Returns true if the code matches; false (never throws for a wrong code) so the caller can surface a clean "incorrect code" message. */
    boolean verify(PendingRegistration registration, OtpChannel channel, String code);
}
