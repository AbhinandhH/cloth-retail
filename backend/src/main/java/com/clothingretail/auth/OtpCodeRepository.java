package com.clothingretail.auth;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OtpCodeRepository extends JpaRepository<OtpCode, Long> {

    List<OtpCode> findByPendingRegistrationIdAndChannelAndConsumedAtIsNull(Long pendingRegistrationId, OtpChannel channel);

    Optional<OtpCode> findFirstByPendingRegistrationIdAndChannelAndConsumedAtIsNullOrderByCreatedAtDesc(
            Long pendingRegistrationId, OtpChannel channel);
}
