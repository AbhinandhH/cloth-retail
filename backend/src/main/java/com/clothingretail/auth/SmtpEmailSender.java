package com.clothingretail.auth;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

/**
 * Real outbound email via SMTP - Spring Boot auto-configures the {@link JavaMailSender} bean from
 * spring.mail.* properties (see application.yml), so this class is just the message-building glue,
 * not a hand-rolled SMTP client. If spring.mail.host is left unconfigured, send() fails at call
 * time with a clear error rather than at startup - the app still boots fine with OTP disabled or
 * before SMTP credentials are supplied.
 */
@Log4j2
@Component
public class SmtpEmailSender implements EmailSender {

    private final JavaMailSender mailSender;
    private final String fromAddress;

    public SmtpEmailSender(
            JavaMailSender mailSender,
            @Value("${app.mail.from:${spring.mail.username:}}") String fromAddress) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
    }

    @Override
    public void send(String to, String subject, String body) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body, false);
            if (fromAddress != null && !fromAddress.isBlank()) {
                helper.setFrom(fromAddress);
            }
            mailSender.send(message);
            log.info("[1950] Email sent to={} subject={}", to, subject);
        } catch (MessagingException | MailException ex) {
            log.error("[1951] Failed to send email to={} subject={} error={}", to, subject, ex.getMessage());
            throw new IllegalStateException("Failed to send verification email - check SMTP configuration", ex);
        }
    }
}
