package com.clothingretail.returns.service;

import com.clothingretail.common.BadRequestException;
import com.clothingretail.common.NotFoundException;
import com.clothingretail.siteconfig.service.MediaValidation;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * Stores damage-claim evidence videos received via WhatsApp (see WhatsAppService) once an admin
 * uploads them - under a directory that is deliberately NEVER registered as a public static
 * resource handler (unlike {@code app.media.upload-dir}/{@code /media/**}, which is fully
 * public). The only way to read these bytes back out is through the two authenticated,
 * ownership-checked evidence-streaming controller endpoints. Video-only (the spec's evidence is
 * always a video) - reuses MediaValidation's video allow-list/cap rather than duplicating it.
 */
@Service
@Log4j2
public class ReturnEvidenceStorageServiceImpl implements ReturnEvidenceStorageService {

    private final Path uploadDir;

    public ReturnEvidenceStorageServiceImpl(@Value("${app.return-evidence.upload-dir}") String uploadDir) {
        this.uploadDir = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    @Override
    public StoredEvidence store(MultipartFile file) {
        log.info("[2000] Storing return-evidence upload: originalFilename={}, contentType={}, size={}",
                file.getOriginalFilename(), file.getContentType(), file.getSize());
        if (file.isEmpty()) {
            log.error("[2001] Evidence upload rejected: file is empty (originalFilename={})", file.getOriginalFilename());
            throw new BadRequestException("File must not be empty");
        }

        String contentType = file.getContentType();
        String extension = MediaValidation.ALLOWED_VIDEO_CONTENT_TYPES.get(contentType);
        if (extension == null) {
            log.error("[2002] Evidence upload rejected: unsupported content type {} (originalFilename={})",
                    contentType, file.getOriginalFilename());
            throw new BadRequestException("Unsupported file type: " + contentType + ". Evidence must be one of: "
                    + String.join(", ", MediaValidation.ALLOWED_VIDEO_CONTENT_TYPES.keySet()));
        }

        if (file.getSize() > MediaValidation.VIDEO_MAX_BYTES) {
            log.error("[2003] Evidence upload rejected: file too large size={} maxBytes={} (originalFilename={})",
                    file.getSize(), MediaValidation.VIDEO_MAX_BYTES, file.getOriginalFilename());
            throw new BadRequestException(
                    "File is too large: evidence videos must not exceed " + (MediaValidation.VIDEO_MAX_BYTES / (1024 * 1024)) + "MB");
        }

        try {
            Files.createDirectories(uploadDir);
            String filename = UUID.randomUUID() + "." + extension;
            file.transferTo(uploadDir.resolve(filename));
            log.info("[2004] Return evidence stored: filename={}, uploadDir={}", filename, uploadDir);
            return new StoredEvidence(filename, contentType);
        } catch (IOException e) {
            log.error("[2005] Failed to store return-evidence upload (originalFilename={}): {}", file.getOriginalFilename(), e.getMessage(), e);
            throw new UncheckedIOException("Failed to store uploaded file", e);
        }
    }

    @Override
    public Resource load(String filename) {
        // filename is always our own UUID-generated name, never a client-supplied path - safe to
        // resolve directly, same trust boundary as MediaStorageServiceImpl.
        Path path = uploadDir.resolve(filename).normalize();
        if (!path.startsWith(uploadDir) || !Files.exists(path)) {
            log.error("[2006] Return evidence file not found: filename={}", filename);
            throw new NotFoundException("Evidence file not found: " + filename);
        }
        return new FileSystemResource(path);
    }
}
