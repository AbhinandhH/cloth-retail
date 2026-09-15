package com.clothingretail.returns.service;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface ReturnEvidenceStorageService {

    record StoredEvidence(String filename, String contentType) {}

    /** Validates (video-only allow-list/cap - see MediaValidation) and stores the file under the private evidence directory. */
    StoredEvidence store(MultipartFile file);

    /** Loads a previously-stored file for streaming back through an authenticated, ownership-checked controller endpoint. */
    Resource load(String filename);
}
