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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Generic image upload used by the site-configuration admin UI (logo,
 * favicon, login/registration visuals, ...). Deliberately narrow: fixed
 * allow-list of image content types, 5MB cap (enforced by Spring's multipart
 * config, see application.yml), files stored under a random UUID name so the
 * original filename - never trusted - can't be used for path traversal or to
 * collide with/overwrite another upload.
 */
@RestController
@RequestMapping("/api/admin/media")
@PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
public class MediaUploadController {

    private static final Map<String, String> ALLOWED_CONTENT_TYPES = Map.of(
            "image/png", "png",
            "image/jpeg", "jpg",
            "image/webp", "webp",
            "image/svg+xml", "svg");

    private final Path uploadDir;

    public MediaUploadController(@Value("${app.media.upload-dir}") String uploadDir) {
        this.uploadDir = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    @PostMapping("/upload")
    public MediaUploadResponse upload(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            throw new BadRequestException("File must not be empty");
        }
        String extension = ALLOWED_CONTENT_TYPES.get(file.getContentType());
        if (extension == null) {
            throw new BadRequestException("Unsupported file type: " + file.getContentType()
                    + ". Allowed types: " + String.join(", ", ALLOWED_CONTENT_TYPES.keySet()));
        }

        try {
            Files.createDirectories(uploadDir);
            String filename = UUID.randomUUID() + "." + extension;
            file.transferTo(uploadDir.resolve(filename));
            return new MediaUploadResponse("/media/" + filename);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to store uploaded file", e);
        }
    }
}
