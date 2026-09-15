package com.clothingretail.notification.controller;

import com.clothingretail.notification.dto.SmtpSettingsResponse;
import com.clothingretail.notification.dto.SmtpSettingsUpdateRequest;
import com.clothingretail.notification.dto.SmtpTestRequest;
import com.clothingretail.notification.dto.SmtpTestResponse;
import com.clothingretail.notification.service.SmtpSettingsService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Admin-configurable SMTP credentials - see SmtpSettings. SUPER_ADMIN only, same as the rest of the Notifications module. */
@RestController
@RequestMapping("/api/admin/smtp-settings")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class AdminSmtpSettingsController {

    private final SmtpSettingsService smtpSettingsService;

    public AdminSmtpSettingsController(SmtpSettingsService smtpSettingsService) {
        this.smtpSettingsService = smtpSettingsService;
    }

    @GetMapping
    public SmtpSettingsResponse get() {
        return smtpSettingsService.get();
    }

    @PutMapping
    public SmtpSettingsResponse update(@Valid @RequestBody SmtpSettingsUpdateRequest request) {
        return smtpSettingsService.update(request);
    }

    @PostMapping("/test")
    public SmtpTestResponse test(@Valid @RequestBody SmtpTestRequest request) {
        return smtpSettingsService.sendTest(request.toEmail());
    }
}
