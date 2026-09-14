-- Adds a second way to send outbound email, alongside raw SMTP - see EmailSenderImpl (renamed
-- from SmtpEmailSender). Cloud hosts commonly block or can't reliably reach raw SMTP ports (25,
-- 465, 587) outbound - confirmed live on this app's own Railway deployment, where both 587 and
-- 465 to smtp.gmail.com timed out at the TCP level, before authentication was ever attempted.
-- Resend's HTTP API sidesteps that entirely, since it's just a normal HTTPS request.
--
-- provider = 'SMTP' (default, every existing install): unchanged raw-SMTP behavior using
-- host/port/username/password/use_starttls below.
-- provider = 'RESEND': api_key + from_address are used instead; host/port/username/password are
-- ignored.
ALTER TABLE smtp_settings ADD COLUMN provider VARCHAR(20) NOT NULL DEFAULT 'SMTP';
ALTER TABLE smtp_settings ADD COLUMN api_key VARCHAR(255);
