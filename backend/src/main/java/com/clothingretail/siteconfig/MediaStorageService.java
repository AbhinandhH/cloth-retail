package com.clothingretail.siteconfig;

import com.clothingretail.common.BadRequestException;
import com.clothingretail.siteconfig.dto.MediaUploadResponse;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;

import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * Validates and persists generic image/video uploads used by the site-configuration
 * admin UI (logo, favicon, login/registration visuals, ...) and by the admin product
 * form's per-variant media list. Fixed allow-list of content types, each with its own
 * size cap enforced here - images are capped tighter (IMAGE_MAX_BYTES) than videos
 * (VIDEO_MAX_BYTES) since a video that size is expected while an image that size
 * usually means something went wrong client-side. Spring's own multipart cap
 * (spring.servlet.multipart.max-file-size, see application.yml) is set to the larger
 * of the two as an outer bound only; it is not what enforces the per-type limit.
 * Files are stored under a random UUID name so the original filename - never trusted
 * - can't be used for path traversal or to collide with/overwrite another upload.
 */
@Service
@Log4j2
public class MediaStorageService {

    private static final long IMAGE_MAX_BYTES = 5L * 1024 * 1024;
    private static final long VIDEO_MAX_BYTES = 50L * 1024 * 1024;

    private static final Map<String, String> ALLOWED_IMAGE_CONTENT_TYPES = Map.of(
            "image/png", "png",
            "image/jpeg", "jpg",
            "image/webp", "webp",
            "image/svg+xml", "svg",
            "image/heic", "heic");

    private static final Map<String, String> ALLOWED_VIDEO_CONTENT_TYPES = Map.of(
            "video/mp4", "mp4",
            "video/webm", "webm",
            "video/quicktime", "mov");

    private final Path uploadDir;

    public MediaStorageService(@Value("${app.media.upload-dir}") String uploadDir) {
        this.uploadDir = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    public MediaUploadResponse store(MultipartFile file) {
        log.info("[1657] Storing uploaded media: originalFilename={}, contentType={}, size={}",
                file.getOriginalFilename(), file.getContentType(), file.getSize());
        if (file.isEmpty()) {
            log.error("[1658] Media upload rejected: file is empty (originalFilename={})", file.getOriginalFilename());
            throw new BadRequestException("File must not be empty");
        }

        String contentType = file.getContentType();
        boolean isVideo = ALLOWED_VIDEO_CONTENT_TYPES.containsKey(contentType);
        String extension = isVideo ? ALLOWED_VIDEO_CONTENT_TYPES.get(contentType) : ALLOWED_IMAGE_CONTENT_TYPES.get(contentType);
        if (extension == null) {
            log.error("[1659] Media upload rejected: unsupported content type {} (originalFilename={})",
                    contentType, file.getOriginalFilename());
            throw new BadRequestException("Unsupported file type: " + contentType + ". Allowed types: "
                    + String.join(", ", ALLOWED_IMAGE_CONTENT_TYPES.keySet())
                    + ", " + String.join(", ", ALLOWED_VIDEO_CONTENT_TYPES.keySet()));
        }

        long maxBytes = isVideo ? VIDEO_MAX_BYTES : IMAGE_MAX_BYTES;
        if (file.getSize() > maxBytes) {
            log.error("[1662] Media upload rejected: file too large size={} maxBytes={} contentType={} (originalFilename={})",
                    file.getSize(), maxBytes, contentType, file.getOriginalFilename());
            throw new BadRequestException(
                    "File is too large: " + (isVideo ? "videos" : "images") + " must not exceed " + (maxBytes / (1024 * 1024)) + "MB");
        }

        try {
            Files.createDirectories(uploadDir);
            String filename = UUID.randomUUID() + "." + extension;
            file.transferTo(uploadDir.resolve(filename));
            log.info("[1660] Media stored: filename={}, uploadDir={}", filename, uploadDir);
            return new MediaUploadResponse("/media/" + filename);
        } catch (IOException e) {
            log.error("[1661] Failed to store uploaded media (originalFilename={}): {}", file.getOriginalFilename(), e.getMessage(), e);
            throw new UncheckedIOException("Failed to store uploaded file", e);
        }
    }
}
