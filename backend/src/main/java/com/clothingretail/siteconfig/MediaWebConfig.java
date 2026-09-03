package com.clothingretail.siteconfig;

import java.nio.file.Path;
import java.nio.file.Paths;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Serves uploaded media files (see MediaUploadController) back out at GET /media/**. */
@Configuration
public class MediaWebConfig implements WebMvcConfigurer {

    private final String uploadDir;

    public MediaWebConfig(@Value("${app.media.upload-dir}") String uploadDir) {
        this.uploadDir = uploadDir;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path resolved = Paths.get(uploadDir).toAbsolutePath().normalize();
        registry.addResourceHandler("/media/**").addResourceLocations("file:" + resolved + "/");
    }
}
