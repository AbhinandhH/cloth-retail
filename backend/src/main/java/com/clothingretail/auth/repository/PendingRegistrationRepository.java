package com.clothingretail.auth.repository;

import com.clothingretail.auth.PendingRegistration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PendingRegistrationRepository extends JpaRepository<PendingRegistration, Long> {

    Optional<PendingRegistration> findByEmailIgnoreCase(String email);

    List<PendingRegistration> findByExpiresAtBefore(Instant cutoff);
}
