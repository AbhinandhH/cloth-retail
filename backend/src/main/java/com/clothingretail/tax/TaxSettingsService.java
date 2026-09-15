package com.clothingretail.tax;

import com.clothingretail.tax.dto.TaxSettingsResponse;
import com.clothingretail.tax.dto.TaxSettingsUpdateRequest;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Admin-configurable GST rates - see TaxSettings. SUPER_ADMIN only, same as SMTP/Notifications/Site configuration. */
@Service
@Transactional(readOnly = true)
@Log4j2
public class TaxSettingsService {

    private final TaxSettingsRepository taxSettingsRepository;

    public TaxSettingsService(TaxSettingsRepository taxSettingsRepository) {
        this.taxSettingsRepository = taxSettingsRepository;
    }

    public TaxSettingsResponse get() {
        return toResponse(loadSingleton());
    }

    @Transactional
    public TaxSettingsResponse update(TaxSettingsUpdateRequest request) {
        log.info("[1993] Updating tax settings cgstPercent={} sgstPercent={}", request.cgstPercent(), request.sgstPercent());
        TaxSettings settings = loadSingleton();
        settings.setCgstPercent(request.cgstPercent());
        settings.setSgstPercent(request.sgstPercent());
        settings = taxSettingsRepository.save(settings);
        log.info("[1994] Tax settings updated cgstPercent={} sgstPercent={}", settings.getCgstPercent(), settings.getSgstPercent());
        return toResponse(settings);
    }

    private TaxSettings loadSingleton() {
        return taxSettingsRepository.findById(TaxSettings.SINGLETON_ID)
                .orElseThrow(() -> {
                    log.error("[1995] Singleton tax_settings row (id={}) is missing", TaxSettings.SINGLETON_ID);
                    return new IllegalStateException(
                            "Singleton tax_settings row (id=1) is missing - this is a startup-time misconfiguration, "
                                    + "check that V24__tax_settings.sql ran");
                });
    }

    private TaxSettingsResponse toResponse(TaxSettings settings) {
        return new TaxSettingsResponse(settings.getCgstPercent(), settings.getSgstPercent());
    }
}
