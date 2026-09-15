package com.clothingretail.returns.service;

import com.clothingretail.siteconfig.SiteConfiguration;
import com.clothingretail.siteconfig.repository.SiteConfigurationRepository;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

/**
 * Zero-cost stand-in for a real WhatsApp Business API integration - no such integration exists
 * anywhere in this codebase today (confirmed: the only prior WhatsApp code is the storefront
 * footer's static {@code wa.me} link), and setting one up needs a real Meta/Twilio/Gupshup
 * account this project doesn't have. Same shape as {@code MockSmsSender} standing in for a real
 * SMS provider: a real implementation of this interface can replace this one later without any
 * caller ever needing to change.
 *
 * <p>How it works: the customer gets a unique reference code plus a {@code wa.me} deep link that
 * opens a chat with the store's own WhatsApp number (reusing {@link SiteConfiguration#getWhatsappNumber()},
 * the same admin-configured number the storefront footer already links to), prefilled with a
 * message containing that code. They send their video there, outside this app entirely, exactly
 * as an ordinary customer would today. Because there's no programmatic way to receive that video
 * back into the system without a real Business API, the admin (who receives it in their own
 * ordinary WhatsApp app) uploads it into the specific return request via the admin portal - the
 * reference code the customer was told to include is what lets the admin find the right request.
 */
@Service
@Log4j2
public class WhatsAppLinkServiceImpl implements WhatsAppService {

    private static final String REFERENCE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int REFERENCE_LENGTH = 8;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final SiteConfigurationRepository siteConfigurationRepository;

    public WhatsAppLinkServiceImpl(SiteConfigurationRepository siteConfigurationRepository) {
        this.siteConfigurationRepository = siteConfigurationRepository;
    }

    @Override
    public String generateReferenceCode() {
        StringBuilder sb = new StringBuilder(REFERENCE_LENGTH + 4);
        sb.append("RET-");
        for (int i = 0; i < REFERENCE_LENGTH; i++) {
            sb.append(REFERENCE_ALPHABET.charAt(RANDOM.nextInt(REFERENCE_ALPHABET.length())));
        }
        String code = sb.toString();
        log.info("[2007] Generated return-evidence reference code: {}", code);
        return code;
    }

    @Override
    public String buildEvidenceLink(String referenceCode) {
        String number = siteConfigurationRepository.findById(SiteConfiguration.SINGLETON_ID)
                .map(SiteConfiguration::getWhatsappNumber)
                .orElse(null);
        if (number == null || number.isBlank()) {
            log.error("[2008] Cannot build WhatsApp evidence link: no whatsappNumber configured in site configuration");
            return null;
        }
        String digitsOnly = number.replaceAll("[^\\d]", "");
        String message = "Damage evidence for return request " + referenceCode
                + " - here is a video of the damaged item.";
        String link = "https://wa.me/" + digitsOnly + "?text=" + URLEncoder.encode(message, StandardCharsets.UTF_8);
        log.info("[2009] Built WhatsApp evidence link for referenceCode={}", referenceCode);
        return link;
    }
}
