package com.clothingretail.notification.controller;

import com.clothingretail.notification.dto.NotificationSettingsResponse;
import com.clothingretail.notification.dto.NotificationSettingsUpdateRequest;
import com.clothingretail.notification.service.NotificationSettingsService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * On/off switches for the OTP notification channels (email/mobile verification at signup) - see
 * NotificationSettings. SUPER_ADMIN only, same as theme/branding control - this affects whether
 * every new customer signup requires a working SMTP/SMS integration, not something delegable to a
 * plain ADMIN account.
 */
@RestController
@RequestMapping("/api/admin/notification-settings")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class AdminNotificationSettingsController {

    private final NotificationSettingsService notificationSettingsService;

    public AdminNotificationSettingsController(NotificationSettingsService notificationSettingsService) {
        this.notificationSettingsService = notificationSettingsService;
    }

    @GetMapping
    public NotificationSettingsResponse get() {
        return notificationSettingsService.get();
    }

    @PutMapping
    public NotificationSettingsResponse update(@Valid @RequestBody NotificationSettingsUpdateRequest request) {
        return notificationSettingsService.update(request);
    }
}
