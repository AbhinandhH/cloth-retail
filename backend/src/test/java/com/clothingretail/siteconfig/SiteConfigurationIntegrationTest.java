package com.clothingretail.siteconfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Exercises the site-configuration module against the Flyway seed data
 * (V4__init_site_configuration.sql: 5 themes, Rose & Charcoal active,
 * business name "ThreadCo").
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SiteConfigurationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private ThemeRepository themeRepository;

    private String superAdminAccessToken() throws Exception {
        String loginBody = """
                {"email":"admin@clothingretail.local","password":"ChangeMe123!"}
                """;
        MvcResult result = mockMvc.perform(post("/api/admin/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return json.get("accessToken").asText();
    }

    /** Creates a fresh plain-ADMIN account (not SUPER_ADMIN) and returns its access token. */
    private String plainAdminAccessToken() throws Exception {
        String superAdminToken = superAdminAccessToken();
        String email = "plain-admin-%s@example.com".formatted(System.nanoTime());
        String createBody = """
                {"fullName":"Plain Admin","email":"%s","password":"Password123!","role":"ADMIN"}
                """.formatted(email);
        mockMvc.perform(post("/api/admin/admins")
                        .header("Authorization", "Bearer " + superAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isOk());

        String loginBody = """
                {"email":"%s","password":"Password123!"}
                """.formatted(email);
        MvcResult result = mockMvc.perform(post("/api/admin/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return json.get("accessToken").asText();
    }

    private String customerAccessToken() throws Exception {
        String registerBody = """
                {"fullName":"Regular Customer","email":"customer-siteconfig-%s@example.com","mobileNumber":"9000000001","password":"Password123!"}
                """.formatted(System.nanoTime());
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return json.get("auth").get("accessToken").asText();
    }

    @Test
    void publicConfigurationReturnsSeededDefaults() throws Exception {
        mockMvc.perform(get("/api/configuration"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.theme.name").value("Rose & Charcoal"))
                .andExpect(jsonPath("$.theme.primaryColor").value("#e11d48"))
                .andExpect(jsonPath("$.theme.secondaryColor").value("#18181b"))
                .andExpect(jsonPath("$.theme.accentColor").doesNotExist())
                .andExpect(jsonPath("$.theme.backgroundColor").value("#fafafa"))
                .andExpect(jsonPath("$.theme.textColor").value("#18181b"))
                .andExpect(jsonPath("$.businessName").value("ThreadCo"))
                .andExpect(jsonPath("$.tagline").value("Everyday fashion, delivered to your door"))
                .andExpect(jsonPath("$.footerText").value("© 2026 ThreadCo. All rights reserved."))
                .andExpect(jsonPath("$.logoUrl").doesNotExist());
    }

    @Test
    void activatingAThemeChangesThePublicConfiguration() throws Exception {
        Theme blackAndGold = themeRepository.findAllByOrderByDisplayOrderAscNameAsc().stream()
                .filter(t -> t.getName().equals("Black & Gold"))
                .findFirst()
                .orElseThrow();
        String token = superAdminAccessToken();

        try {
            mockMvc.perform(post("/api/admin/themes/{id}/activate", blackAndGold.getId())
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.theme.name").value("Black & Gold"))
                    .andExpect(jsonPath("$.theme.primaryColor").value("#000000"));

            mockMvc.perform(get("/api/configuration"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.theme.name").value("Black & Gold"));
        } finally {
            // Restore the default active theme so other tests in this class (and any
            // other test relying on the seeded default) aren't affected by ordering.
            Theme roseAndCharcoal = themeRepository.findAllByOrderByDisplayOrderAscNameAsc().stream()
                    .filter(t -> t.getName().equals("Rose & Charcoal"))
                    .findFirst()
                    .orElseThrow();
            mockMvc.perform(post("/api/admin/themes/{id}/activate", roseAndCharcoal.getId())
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk());
        }
    }

    @Test
    void nonSuperAdminCannotAccessThemeOrConfigurationAdminEndpoints() throws Exception {
        String customerToken = customerAccessToken();
        String plainAdminToken = plainAdminAccessToken();

        mockMvc.perform(get("/api/admin/themes").header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/admin/configuration").header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/admin/themes").header("Authorization", "Bearer " + plainAdminToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/admin/configuration").header("Authorization", "Bearer " + plainAdminToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void mediaUploadRejectsDisallowedContentTypeAndServesAllowedOne() throws Exception {
        String token = superAdminAccessToken();

        MockMultipartFile disallowed =
                new MockMultipartFile("file", "notes.txt", MediaType.TEXT_PLAIN_VALUE, "hello".getBytes());
        mockMvc.perform(multipart("/api/admin/media/upload")
                        .file(disallowed)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());

        MockMultipartFile allowed =
                new MockMultipartFile("file", "logo.png", MediaType.IMAGE_PNG_VALUE, new byte[] {1, 2, 3, 4});
        MvcResult uploadResult = mockMvc.perform(multipart("/api/admin/media/upload")
                        .file(allowed)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode uploadJson = objectMapper.readTree(uploadResult.getResponse().getContentAsString());
        String url = uploadJson.get("url").asText();
        assertThat(url).matches("^/media/[0-9a-f-]{36}\\.png$");

        mockMvc.perform(get(url)).andExpect(status().isOk());
    }

    @Test
    void mediaUploadAcceptsVideoAndEnforcesSeparatePerTypeSizeCaps() throws Exception {
        String token = superAdminAccessToken();

        // A valid video content type uploads successfully, same endpoint as images.
        MockMultipartFile video =
                new MockMultipartFile("file", "clip.mp4", "video/mp4", new byte[] {1, 2, 3, 4});
        MvcResult uploadResult = mockMvc.perform(multipart("/api/admin/media/upload")
                        .file(video)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode uploadJson = objectMapper.readTree(uploadResult.getResponse().getContentAsString());
        String url = uploadJson.get("url").asText();
        assertThat(url).matches("^/media/[0-9a-f-]{36}\\.mp4$");
        mockMvc.perform(get(url)).andExpect(status().isOk());

        // An unsupported video-ish content type is still rejected.
        MockMultipartFile badVideoType =
                new MockMultipartFile("file", "clip.avi", "video/x-msvideo", new byte[] {1, 2, 3, 4});
        mockMvc.perform(multipart("/api/admin/media/upload")
                        .file(badVideoType)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());

        // An image over the 5MB image cap is rejected even though it's well under the video cap.
        byte[] oversizedImageBytes = new byte[6 * 1024 * 1024];
        MockMultipartFile oversizedImage =
                new MockMultipartFile("file", "big.png", MediaType.IMAGE_PNG_VALUE, oversizedImageBytes);
        mockMvc.perform(multipart("/api/admin/media/upload")
                        .file(oversizedImage)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());

        // A video under the 50MB video cap (but well over the 5MB image cap) is accepted -
        // proves the video cap, not the image cap, is what's applied to video uploads.
        byte[] largeVideoBytes = new byte[10 * 1024 * 1024];
        MockMultipartFile largeVideo =
                new MockMultipartFile("file", "big.mp4", "video/mp4", largeVideoBytes);
        mockMvc.perform(multipart("/api/admin/media/upload")
                        .file(largeVideo)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }
}
