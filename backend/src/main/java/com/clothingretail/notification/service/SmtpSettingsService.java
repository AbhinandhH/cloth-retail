package com.clothingretail.notification.service;

import com.clothingretail.notification.dto.SmtpSettingsResponse;
import com.clothingretail.notification.dto.SmtpSettingsUpdateRequest;
import com.clothingretail.notification.dto.SmtpTestResponse;

public interface SmtpSettingsService {

    SmtpSettingsResponse get();

    SmtpSettingsResponse update(SmtpSettingsUpdateRequest request);

    /** Always returns 200 with success/message rather than letting the real failure get flattened by GlobalExceptionHandler's catch-all. */
    SmtpTestResponse sendTest(String toEmail);
}
