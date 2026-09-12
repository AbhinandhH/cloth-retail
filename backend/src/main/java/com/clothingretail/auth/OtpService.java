package com.clothingretail.auth;

import com.clothingretail.common.ConflictException;
import com.clothingretail.notification.NotificationSettings;
import com.clothingretail.notification.NotificationSettingsRepository;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.List;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Generates, sends, and verifies OTP codes for email/mobile verification at signup. Codes are
 * never stored in plaintext (see {@link OtpCode}'s own doc comment) - only their SHA-256 hash,
 * reusing {@link JwtService#hashToken} since it's a generic string hash, not JWT-specific.
 */
@Service
@Log4j2
public class OtpService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final OtpCodeRepository otpCodeRepository;
    private final JwtService jwtService;
    private final EmailSender emailSender;
    private final SmsSender smsSender;
    private final NotificationSettingsRepository notificationSettingsRepository;

    private final int codeLength;
    private final int ttlMinutes;
    private final int maxAttempts;
    private final int resendCooldownSeconds;

    public OtpService(
            OtpCodeRepository otpCodeRepository,
            JwtService jwtService,
            EmailSender emailSender,
            SmsSender smsSender,
            NotificationSettingsRepository notificationSettingsRepository,
            @Value("${app.otp.code-length:6}") int codeLength,
            @Value("${app.otp.ttl-minutes:10}") int ttlMinutes,
            @Value("${app.otp.max-attempts:5}") int maxAttempts,
            @Value("${app.otp.resend-cooldown-seconds:30}") int resendCooldownSeconds) {
        this.otpCodeRepository = otpCodeRepository;
        this.jwtService = jwtService;
        this.emailSender = emailSender;
        this.smsSender = smsSender;
        this.notificationSettingsRepository = notificationSettingsRepository;
        this.codeLength = codeLength;
        this.ttlMinutes = ttlMinutes;
        this.maxAttempts = maxAttempts;
        this.resendCooldownSeconds = resendCooldownSeconds;
    }

    /** Invalidates any outstanding code for this (pending registration, channel), generates a fresh one, and sends it. */
    @Transactional
    public void generateAndSend(PendingRegistration registration, OtpChannel channel) {
        List<OtpCode> outstanding =
                otpCodeRepository.findByPendingRegistrationIdAndChannelAndConsumedAtIsNull(registration.getId(), channel);
        Instant now = Instant.now();
        boolean recentlyIssued = outstanding.stream()
                .anyMatch(o -> o.getCreatedAt() != null && o.getCreatedAt().isAfter(now.minusSeconds(resendCooldownSeconds)));
        if (recentlyIssued) {
            log.error("[1953] OTP resend rejected, cooldown active registrationId={} channel={}", registration.getId(), channel);
            throw new ConflictException("Please wait a moment before requesting another code");
        }
        outstanding.forEach(o -> o.setConsumedAt(now));
        otpCodeRepository.saveAll(outstanding);

        String code = generateCode();
        OtpCode otp = new OtpCode();
        otp.setPendingRegistration(registration);
        otp.setChannel(channel);
        otp.setCodeHash(jwtService.hashToken(code));
        otp.setExpiresAt(now.plusSeconds(ttlMinutes * 60L));
        otpCodeRepository.save(otp);
        log.info("[1954] OTP generated registrationId={} channel={} expiresAt={}", registration.getId(), channel, otp.getExpiresAt());

        NotificationSettings settings = loadNotificationSettings();
        String message = renderMessage(settings.getMessageTemplate(), code);
        if (channel == OtpChannel.EMAIL) {
            emailSender.send(registration.getEmail(), settings.getEmailSubject(), message);
        } else {
            smsSender.send(registration.getMobileNumber(), message);
        }
    }

    /** Substitutes {code}/{ttlMinutes} into the admin-configured template - see NotificationSettings.messageTemplate. */
    private String renderMessage(String template, String code) {
        return template.replace("{code}", code).replace("{ttlMinutes}", String.valueOf(ttlMinutes));
    }

    private NotificationSettings loadNotificationSettings() {
        return notificationSettingsRepository.findById(NotificationSettings.SINGLETON_ID)
                .orElseThrow(() -> {
                    log.error("[1990] Singleton notification_settings row (id={}) is missing", NotificationSettings.SINGLETON_ID);
                    return new IllegalStateException(
                            "Singleton notification_settings row (id=1) is missing - this is a startup-time misconfiguration, "
                                    + "check that V16__notification_settings.sql ran");
                });
    }

    /** Returns true if the code matches; false (never throws for a wrong code) so the caller can surface a clean "incorrect code" message. */
    @Transactional
    public boolean verify(PendingRegistration registration, OtpChannel channel, String code) {
        OtpCode otp = otpCodeRepository
                .findFirstByPendingRegistrationIdAndChannelAndConsumedAtIsNullOrderByCreatedAtDesc(registration.getId(), channel)
                .orElse(null);
        if (otp == null || otp.isExpired()) {
            log.error("[1955] OTP verify failed, no active code registrationId={} channel={}", registration.getId(), channel);
            throw new ConflictException("No active code for this - request a new one");
        }
        if (otp.getAttemptCount() >= maxAttempts) {
            log.error("[1956] OTP verify failed, too many attempts registrationId={} channel={}", registration.getId(), channel);
            throw new ConflictException("Too many incorrect attempts - request a new code");
        }
        otp.setAttemptCount(otp.getAttemptCount() + 1);
        boolean matches = jwtService.hashToken(code).equals(otp.getCodeHash());
        if (!matches) {
            otpCodeRepository.save(otp);
            log.error("[1957] OTP verify failed, code mismatch registrationId={} channel={} attempt={}",
                    registration.getId(), channel, otp.getAttemptCount());
            return false;
        }
        otp.setConsumedAt(Instant.now());
        otpCodeRepository.save(otp);
        log.info("[1958] OTP verified registrationId={} channel={}", registration.getId(), channel);
        return true;
    }

    private String generateCode() {
        StringBuilder sb = new StringBuilder(codeLength);
        for (int i = 0; i < codeLength; i++) {
            sb.append(RANDOM.nextInt(10));
        }
        return sb.toString();
    }
}
