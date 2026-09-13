package com.clothingretail.notification;

import com.clothingretail.auth.EmailSender;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.util.Properties;
import lombok.extern.log4j.Log4j2;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

/**
 * Real outbound email via SMTP. Deliberately does NOT rely on Spring Boot's spring.mail.*
 * auto-configured {@link org.springframework.mail.javamail.JavaMailSender} bean (fixed at
 * startup) - instead builds a fresh {@link JavaMailSenderImpl} from {@link SmtpSettings} on every
 * send, so an admin changing the SMTP config in the Notifications module takes effect on the very
 * next email, no restart needed. If unconfigured (no host set), send() fails at call time with a
 * clear error rather than at startup - the app still boots fine with OTP disabled or before SMTP
 * credentials are supplied.
 */
@Log4j2
@Component
public class SmtpEmailSender implements EmailSender {

    // Depends on the repository directly, not SmtpSettingsService - that service itself depends
    // on EmailSender (to send test emails), and going through it here would be a circular bean
    // dependency (SmtpEmailSender -> SmtpSettingsService -> EmailSender -> SmtpEmailSender).
    private final SmtpSettingsRepository smtpSettingsRepository;

    public SmtpEmailSender(SmtpSettingsRepository smtpSettingsRepository) {
        this.smtpSettingsRepository = smtpSettingsRepository;
    }

    @Override
    public void send(String to, String subject, String body) {
        SmtpSettings settings = smtpSettingsRepository.findById(SmtpSettings.SINGLETON_ID)
                .orElseThrow(() -> {
                    log.error("[1986] Singleton smtp_settings row (id={}) is missing", SmtpSettings.SINGLETON_ID);
                    return new IllegalStateException(
                            "Singleton smtp_settings row (id=1) is missing - this is a startup-time misconfiguration, "
                                    + "check that V18__smtp_settings.sql ran");
                });
        if (!settings.isConfigured()) {
            log.error("[1951] Failed to send email to={} subject={} error=SMTP not configured", to, subject);
            throw new IllegalStateException("SMTP is not configured - set it up in the admin Notifications module");
        }
        try {
            JavaMailSenderImpl mailSender = buildSender(settings);
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body, false);
            String from = settings.getFromAddress() != null && !settings.getFromAddress().isBlank()
                    ? settings.getFromAddress()
                    : settings.getUsername();
            if (from != null && !from.isBlank()) {
                helper.setFrom(from);
            }
            mailSender.send(message);
            log.info("[1950] Email sent to={} subject={}", to, subject);
        } catch (MessagingException | MailException ex) {
            log.error("[1951] Failed to send email to={} subject={} error={}", to, subject, ex.getMessage());
            throw new IllegalStateException("Failed to send verification email - check SMTP configuration", ex);
        }
    }

    private JavaMailSenderImpl buildSender(SmtpSettings settings) {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(settings.getHost());
        mailSender.setPort(settings.getPort());
        mailSender.setUsername(settings.getUsername());
        mailSender.setPassword(settings.getPassword());
        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", String.valueOf(settings.isUseStarttls()));
        // Without these, an unreachable/misconfigured SMTP host (wrong port, blocked outbound
        // connection, typo'd hostname) hangs this call indefinitely - JavaMailSenderImpl sets no
        // timeout by default. That's not just slow: it blocks the calling request thread forever,
        // which for a caller like registration (see OtpService.generateAndSend, called
        // synchronously from AuthService.register) means the customer's signup request itself
        // never returns. 10s each is generous for a real SMTP handshake but still fails fast
        // enough to surface as a clear error instead of an indefinite hang.
        props.put("mail.smtp.connectiontimeout", "10000");
        props.put("mail.smtp.timeout", "10000");
        props.put("mail.smtp.writetimeout", "10000");
        return mailSender;
    }
}
