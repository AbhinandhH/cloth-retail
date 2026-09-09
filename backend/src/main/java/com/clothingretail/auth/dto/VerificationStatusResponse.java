package com.clothingretail.auth.dto;

/**
 * Shared response shape for /auth/register and /auth/otp/verify. When a signup still needs one or
 * more OTP channels confirmed, {@code completed} is false, {@code auth} is null, and
 * email/mobileVerificationRequired reflect what's still outstanding (recomputed fresh each call,
 * not just at registration) so the frontend knows which OTP inputs to show. {@code registrationId}
 * identifies the pending signup (see PendingRegistration) while completed is false - it's what the
 * caller passes back into /auth/otp/verify and /auth/otp/resend. Once every required channel is
 * verified, {@code completed} is true and {@code auth} carries the real tokens - this is the point
 * a real account is actually created and the caller should log the user in (see
 * AuthService.verifyOtp).
 */
public record VerificationStatusResponse(
        boolean completed,
        Long registrationId,
        boolean emailVerificationRequired,
        boolean mobileVerificationRequired,
        AuthResponse auth) {}
