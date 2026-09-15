package com.clothingretail.tax.service;

import com.clothingretail.tax.dto.TaxSettingsResponse;
import com.clothingretail.tax.dto.TaxSettingsUpdateRequest;

/** Admin-configurable GST rates - see com.clothingretail.tax.TaxSettings. SUPER_ADMIN only, same as SMTP/Notifications/Site configuration. */
public interface TaxSettingsService {

    TaxSettingsResponse get();

    TaxSettingsResponse update(TaxSettingsUpdateRequest request);
}
