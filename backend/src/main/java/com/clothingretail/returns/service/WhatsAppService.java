package com.clothingretail.returns.service;

/**
 * Abstraction for the damaged-product evidence WhatsApp workflow - kept as a real interface (not
 * just a method on ReturnRequestServiceImpl) so a genuine WhatsApp Business API/Cloud API
 * integration can be swapped in later, if one is ever set up, without touching any caller. See
 * {@link WhatsAppLinkServiceImpl} for why the current implementation is a deep-link, not a real
 * API call.
 */
public interface WhatsAppService {

    /** A short, unguessable token the customer includes in their WhatsApp message so an admin can correlate it back to the right request. */
    String generateReferenceCode();

    /** A tap-to-chat link that opens WhatsApp with the store's number and a prefilled message including {@code referenceCode}. */
    String buildEvidenceLink(String referenceCode);
}
