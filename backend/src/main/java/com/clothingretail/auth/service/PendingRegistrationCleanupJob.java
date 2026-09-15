package com.clothingretail.auth.service;

import com.clothingretail.auth.PendingRegistration;
import com.clothingretail.auth.repository.PendingRegistrationRepository;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Every hour, deletes pending signups that were never completed (see PendingRegistration -
 * whose whole point is that an abandoned signup leaves nothing durable behind, not even a
 * half-finished row lingering forever). Their otp_codes rows cascade-delete along with them.
 */
@Component
public class PendingRegistrationCleanupJob {

    private static final Logger log = LoggerFactory.getLogger(PendingRegistrationCleanupJob.class);

    private final PendingRegistrationRepository pendingRegistrationRepository;

    public PendingRegistrationCleanupJob(PendingRegistrationRepository pendingRegistrationRepository) {
        this.pendingRegistrationRepository = pendingRegistrationRepository;
    }

    @Scheduled(fixedDelay = 3600000)
    @Transactional
    public void deleteExpired() {
        List<PendingRegistration> expired = pendingRegistrationRepository.findByExpiresAtBefore(Instant.now());
        if (expired.isEmpty()) {
            return;
        }
        pendingRegistrationRepository.deleteAll(expired);
        log.info("Deleted {} expired pending registration(s)", expired.size());
    }
}
