package com.clothingretail.auth.service;

/** Outbound SMS, abstracted so the OTP flow doesn't depend on a specific provider. */
public interface SmsSender {
    void send(String toMobileNumber, String message);
}
