package com.clothingretail.auth;

/** Outbound email, abstracted so the OTP flow doesn't depend on a specific provider. */
public interface EmailSender {
    void send(String to, String subject, String body);
}
