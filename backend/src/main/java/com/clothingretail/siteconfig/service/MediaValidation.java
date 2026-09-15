package com.clothingretail.siteconfig.service;

import java.util.Map;

/**
 * Shared content-type/size allow-list, extracted out of {@link MediaStorageServiceImpl} so the
 * private return-evidence storage service (see {@code com.clothingretail.returns.service.
 * ReturnEvidenceStorageServiceImpl}) can reuse the same video allow-list/cap without duplicating
 * the literals. Storage *location* and *security* stay fully independent between the two
 * services - this class only holds the validation rules both happen to need.
 */
public final class MediaValidation {

    public static final long IMAGE_MAX_BYTES = 5L * 1024 * 1024;
    public static final long VIDEO_MAX_BYTES = 50L * 1024 * 1024;

    public static final Map<String, String> ALLOWED_IMAGE_CONTENT_TYPES = Map.of(
            "image/png", "png",
            "image/jpeg", "jpg",
            "image/webp", "webp",
            "image/svg+xml", "svg",
            "image/heic", "heic");

    public static final Map<String, String> ALLOWED_VIDEO_CONTENT_TYPES = Map.of(
            "video/mp4", "mp4",
            "video/webm", "webm",
            "video/quicktime", "mov");

    private MediaValidation() {}
}
