package com.clothingretail.siteconfig.service;

import com.clothingretail.siteconfig.dto.MediaUploadResponse;
import org.springframework.web.multipart.MultipartFile;

/**
 * Validates and persists generic image/video uploads used by the site-configuration
 * admin UI (logo, favicon, login/registration visuals, ...) and by the admin product
 * form's per-variant media list.
 */
public interface MediaStorageService {

    MediaUploadResponse store(MultipartFile file);
}
