package com.clothingretail.notification;

import com.clothingretail.notification.dto.NotificationSettingsResponse;
import com.clothingretail.notification.dto.NotificationSettingsUpdateRequest;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@Log4j2
public class NotificationSettingsService {

    private final NotificationSettingsRepository notificationSettingsRepository;

    public NotificationSettingsService(NotificationSettingsRepository notificationSettingsRepository) {
        this.notificationSettingsRepository = notificationSettingsRepository;
    }

    public NotificationSettingsResponse get() {
        NotificationSettings settings = loadSingleton();
        return toResponse(settings);
    }

    @Transactional
    public NotificationSettingsResponse update(NotificationSettingsUpdateRequest request) {
        log.info("[1970] Updating notification settings emailVerificationEnabled={} mobileVerificationEnabled={}",
                request.emailVerificationEnabled(), request.mobileVerificationEnabled());
        NotificationSettings settings = loadSingleton();
        settings.setEmailVerificationEnabled(request.emailVerificationEnabled());
        settings.setMobileVerificationEnabled(request.mobileVerificationEnabled());
        settings.setEmailSubject(request.emailSubject());
        settings.setMessageTemplate(request.messageTemplate());
        settings = notificationSettingsRepository.save(settings);
        log.info("[1971] Notification settings updated emailVerificationEnabled={} mobileVerificationEnabled={}",
                settings.isEmailVerificationEnabled(), settings.isMobileVerificationEnabled());
        return toResponse(settings);
    }

    NotificationSettings loadSingleton() {
        return notificationSettingsRepository.findById(NotificationSettings.SINGLETON_ID)
                .orElseThrow(() -> {
                    log.error("[1972] Singleton notification_settings row (id={}) is missing", NotificationSettings.SINGLETON_ID);
                    return new IllegalStateException(
                            "Singleton notification_settings row (id=1) is missing - this is a startup-time misconfiguration, "
                                    + "check that V16__notification_settings.sql ran");
                });
    }

    private NotificationSettingsResponse toResponse(NotificationSettings settings) {
        return new NotificationSettingsResponse(
                settings.isEmailVerificationEnabled(),
                settings.isMobileVerificationEnabled(),
                settings.getEmailSubject(),
                settings.getMessageTemplate());
    }
}
