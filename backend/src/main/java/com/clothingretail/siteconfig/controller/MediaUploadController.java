package com.clothingretail.siteconfig.controller;

import com.clothingretail.siteconfig.dto.MediaUploadResponse;
import com.clothingretail.siteconfig.service.MediaStorageService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Generic image upload used by the site-configuration admin UI (logo,
 * favicon, login/registration visuals, ...). See MediaStorageService for the
 * validation/storage rules.
 */
@RestController
@RequestMapping("/api/admin/media")
@PreAuthorize("hasRole('ADMIN') or (hasRole('EMPLOYEE') and @modulePermission.hasAccess('PRODUCTS'))")
public class MediaUploadController {

    private final MediaStorageService mediaStorageService;

    public MediaUploadController(MediaStorageService mediaStorageService) {
        this.mediaStorageService = mediaStorageService;
    }

    @PostMapping("/upload")
    public MediaUploadResponse upload(@RequestParam("file") MultipartFile file) {
        return mediaStorageService.store(file);
    }
}
