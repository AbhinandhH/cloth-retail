package com.clothingretail.notification;

import com.clothingretail.auth.EmailSender;
import com.clothingretail.notification.dto.SmtpSettingsResponse;
import com.clothingretail.notification.dto.SmtpSettingsUpdateRequest;
import com.clothingretail.notification.dto.SmtpTestResponse;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@Log4j2
public class SmtpSettingsService {

    private final SmtpSettingsRepository smtpSettingsRepository;
    private final EmailSender emailSender;

    public SmtpSettingsService(SmtpSettingsRepository smtpSettingsRepository, EmailSender emailSender) {
        this.smtpSettingsRepository = smtpSettingsRepository;
        this.emailSender = emailSender;
    }

    public SmtpSettingsResponse get() {
        return toResponse(loadSingleton());
    }

    @Transactional
    public SmtpSettingsResponse update(SmtpSettingsUpdateRequest request) {
        log.info("[1980] Updating SMTP settings host={} port={} username={} useStarttls={}",
                request.host(), request.port(), request.username(), request.useStarttls());
        SmtpSettings settings = loadSingleton();
        settings.setHost(blankToNull(request.host()));
        settings.setPort(request.port());
        settings.setUsername(blankToNull(request.username()));
        // Blank/absent password means "keep what's already stored" - see the DTO's own doc
        // comment - so an admin tweaking the from-address doesn't have to re-paste the app
        // password every time. Whitespace is stripped from a new one: Google's own UI displays
        // an app password as 4 space-separated groups ("abcd efgh ijkl mnop") purely for
        // readability - the real 16-character secret has no spaces in it, and pasting it verbatim
        // (spaces included) is a common cause of an otherwise-correct app password failing auth.
        if (request.password() != null && !request.password().isBlank()) {
            settings.setPassword(request.password().replaceAll("\\s+", ""));
        }
        settings.setFromAddress(blankToNull(request.fromAddress()));
        settings.setUseStarttls(request.useStarttls());
        settings = smtpSettingsRepository.save(settings);
        log.info("[1981] SMTP settings updated host={} configured={}", settings.getHost(), settings.isConfigured());
        return toResponse(settings);
    }

    /** Always returns 200 with success/message rather than letting the real failure get flattened by GlobalExceptionHandler's catch-all. */
    public SmtpTestResponse sendTest(String toEmail) {
        log.info("[1982] Sending SMTP test email to={}", toEmail);
        try {
            emailSender.send(toEmail, "Test email from Loom Atelier Studio",
                    "This is a test email confirming your SMTP settings are working correctly.");
            log.info("[1983] SMTP test email succeeded to={}", toEmail);
            return new SmtpTestResponse(true, "Test email sent to " + toEmail + " - check the inbox (and spam folder).");
        } catch (Exception ex) {
            String reason = ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage();
            log.error("[1984] SMTP test email failed to={} reason={}", toEmail, reason);
            return new SmtpTestResponse(false, reason != null ? reason : "Failed to send test email");
        }
    }

    SmtpSettings loadSingleton() {
        return smtpSettingsRepository.findById(SmtpSettings.SINGLETON_ID)
                .orElseThrow(() -> {
                    log.error("[1985] Singleton smtp_settings row (id={}) is missing", SmtpSettings.SINGLETON_ID);
                    return new IllegalStateException(
                            "Singleton smtp_settings row (id=1) is missing - this is a startup-time misconfiguration, "
                                    + "check that V18__smtp_settings.sql ran");
                });
    }

    private String blankToNull(String value) {
        return value != null && !value.isBlank() ? value : null;
    }

    private SmtpSettingsResponse toResponse(SmtpSettings settings) {
        return new SmtpSettingsResponse(
                settings.getHost(),
                settings.getPort(),
                settings.getUsername(),
                settings.getPassword() != null && !settings.getPassword().isBlank(),
                settings.getFromAddress(),
                settings.isUseStarttls());
    }
}
