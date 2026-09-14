package com.clothingretail.notification;

import com.clothingretail.auth.EmailSender;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import lombok.extern.log4j.Log4j2;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

/**
 * Real outbound email, via either raw SMTP or the Resend HTTP API depending on the admin-chosen
 * {@link SmtpSettings#getProvider()}. Deliberately does NOT rely on Spring Boot's spring.mail.*
 * auto-configured {@link org.springframework.mail.javamail.JavaMailSender} bean (fixed at
 * startup) - instead builds a fresh sender from {@link SmtpSettings} on every send, so an admin
 * changing the config in the Notifications module takes effect on the very next email, no restart
 * needed. If unconfigured, send() fails at call time with a clear error rather than at startup -
 * the app still boots fine with OTP disabled or before credentials are supplied.
 *
 * <p>The Resend path exists because raw SMTP is commonly blocked or unreachable outbound from
 * PaaS hosts - confirmed live on this app's own Railway deployment, where both port 587
 * (STARTTLS) and 465 (SSL) to smtp.gmail.com timed out at the TCP level, before authentication
 * was ever attempted. Resend's API is a plain HTTPS POST, which is never blocked the way raw SMTP
 * ports are.
 */
@Log4j2
@Component
public class EmailSenderImpl implements EmailSender {

    private static final URI RESEND_API_URI = URI.create("https://api.resend.com/emails");
    private static final Duration HTTP_TIMEOUT = Duration.ofSeconds(10);

    // Depends on the repository directly, not SmtpSettingsService - that service itself depends
    // on EmailSender (to send test emails), and going through it here would be a circular bean
    // dependency (EmailSenderImpl -> SmtpSettingsService -> EmailSender -> EmailSenderImpl).
    private final SmtpSettingsRepository smtpSettingsRepository;
    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(HTTP_TIMEOUT).build();
    // Same reasoning as MockPaymentGateway/PaymentWebhookService's own ObjectMapper field: this
    // Spring Boot version auto-configures a tools.jackson.databind.ObjectMapper bean, not the
    // classic com.fasterxml.jackson.databind.ObjectMapper used here - so there's no Spring-managed
    // bean of this type to inject, hence a plain local instance instead.
    private final ObjectMapper objectMapper = new ObjectMapper();

    public EmailSenderImpl(SmtpSettingsRepository smtpSettingsRepository) {
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
            log.error("[1951] Failed to send email to={} subject={} error=not configured provider={}", to, subject, settings.getProvider());
            throw new IllegalStateException("Email sending is not configured - set it up in the admin Notifications module");
        }
        if ("RESEND".equals(settings.getProvider())) {
            sendViaResend(settings, to, subject, body);
        } else {
            sendViaSmtp(settings, to, subject, body);
        }
    }

    private void sendViaResend(SmtpSettings settings, String to, String subject, String body) {
        String from = settings.getFromAddress() != null && !settings.getFromAddress().isBlank()
                ? settings.getFromAddress()
                : "onboarding@resend.dev";
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("from", from);
            payload.put("to", List.of(to));
            payload.put("subject", subject);
            payload.put("text", body);
            String json = objectMapper.writeValueAsString(payload);

            HttpRequest request = HttpRequest.newBuilder(RESEND_API_URI)
                    .timeout(HTTP_TIMEOUT)
                    .header("Authorization", "Bearer " + settings.getApiKey())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 != 2) {
                String reason = extractResendError(response.body());
                log.error("[1951] Failed to send email via Resend to={} subject={} status={} body={}",
                        to, subject, response.statusCode(), response.body());
                throw new IllegalStateException("Resend API error: " + reason);
            }
            log.info("[1950] Email sent via Resend to={} subject={}", to, subject);
        } catch (IOException | InterruptedException ex) {
            if (ex instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            log.error("[1951] Failed to send email via Resend to={} subject={} error={}", to, subject, ex.getMessage());
            throw new IllegalStateException("Failed to reach Resend - check your network/API key", ex);
        }
    }

    /** Resend's error body is JSON like {"message": "..."}; falls back to the raw body if it isn't. */
    private String extractResendError(String responseBody) {
        try {
            JsonNode node = objectMapper.readTree(responseBody);
            JsonNode message = node.get("message");
            if (message != null && !message.isNull()) {
                return message.asText();
            }
        } catch (IOException ignored) {
            // Not JSON, or unexpected shape - fall through to the raw body below.
        }
        return responseBody;
    }

    private void sendViaSmtp(SmtpSettings settings, String to, String subject, String body) {
        try {
            JavaMailSenderImpl mailSender = buildSmtpSender(settings);
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
            log.info("[1950] Email sent via SMTP to={} subject={}", to, subject);
        } catch (MessagingException | MailException ex) {
            log.error("[1951] Failed to send email via SMTP to={} subject={} error={}", to, subject, ex.getMessage());
            throw new IllegalStateException("Failed to send verification email - check SMTP configuration", ex);
        }
    }

    private JavaMailSenderImpl buildSmtpSender(SmtpSettings settings) {
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
