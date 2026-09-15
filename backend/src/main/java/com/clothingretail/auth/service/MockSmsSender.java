package com.clothingretail.auth.service;

import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;

/**
 * Stands in for a real SMS provider (Twilio, MSG91, etc.) - logs the message instead of sending it
 * over the network. No SMS provider account/API key has been set up for this project; swapping in
 * a real implementation later just means writing a new {@link SmsSender} bean (e.g. calling
 * Twilio's API) and removing this one - {@link OtpService} depends only on the interface.
 */
@Log4j2
@Component
public class MockSmsSender implements SmsSender {

    @Override
    public void send(String toMobileNumber, String message) {
        log.info("[1952] [MOCK SMS] to={} message=\"{}\"", toMobileNumber, message);
    }
}
