package com.clothingretail.notification.service;

import com.clothingretail.notification.dto.NotificationSettingsResponse;
import com.clothingretail.notification.dto.NotificationSettingsUpdateRequest;

public interface NotificationSettingsService {

    NotificationSettingsResponse get();

    NotificationSettingsResponse update(NotificationSettingsUpdateRequest request);
}
