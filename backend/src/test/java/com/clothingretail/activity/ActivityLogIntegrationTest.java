package com.clothingretail.activity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.clothingretail.activity.repository.ActivityLogRepository;
import com.clothingretail.support.CheckoutTestSupport;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/** Covers ActivityLoggingInterceptor: a mutating admin call is logged, a GET is not, and the admin-only listing endpoint returns what was logged. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ActivityLogIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private ActivityLogRepository activityLogRepository;

    @Test
    void creatingABrandIsLoggedButListingBrandsIsNot() throws Exception {
        String token = CheckoutTestSupport.adminAccessToken(mockMvc, objectMapper);
        String uniqueName = "Activity Log Test Brand " + UUID.randomUUID();

        long countBefore = activityLogRepository.count();

        // A GET must not be logged.
        mockMvc.perform(get("/api/admin/brands").header("Authorization", "Bearer " + token)).andExpect(status().isOk());
        assertThat(activityLogRepository.count()).isEqualTo(countBefore);

        // A mutating POST must be logged.
        String createBody = """
                {"name":"%s","displayOrder":99,"active":true}
                """.formatted(uniqueName);
        MvcResult createResult = mockMvc.perform(post("/api/admin/brands")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isOk())
                .andReturn();
        long brandId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asLong();

        List<ActivityLog> rows = activityLogRepository.findAll();
        ActivityLog logged = rows.stream()
                .filter(r -> r.getPath().equals("/api/admin/brands") && r.getHttpMethod().equals("POST"))
                .reduce((first, second) -> second) // most recent match
                .orElseThrow(() -> new AssertionError("No POST /api/admin/brands activity row found"));
        assertThat(logged.getModule()).isEqualTo("brands");
        assertThat(logged.getStatusCode()).isEqualTo(200);
        assertThat(logged.getActorId()).isNotNull();

        MvcResult listResult = mockMvc.perform(get("/api/admin/activity-log")
                        .header("Authorization", "Bearer " + token)
                        .param("module", "brands")
                        .param("httpMethod", "POST"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode listJson = objectMapper.readTree(listResult.getResponse().getContentAsString());
        assertThat(listJson.get("content").isArray()).isTrue();
        assertThat(listJson.get("content").size()).isGreaterThan(0);
        assertThat(listJson.get("content").get(0).get("actorName").asText()).isNotBlank();

        assertThat(brandId).isGreaterThan(0);
    }

    @Test
    void nonAdminCannotReadTheActivityLog() throws Exception {
        var customer = CheckoutTestSupport.registerCustomer(mockMvc, objectMapper, "activity-log-guard");
        mockMvc.perform(get("/api/admin/activity-log").header("Authorization", "Bearer " + customer.accessToken()))
                .andExpect(status().isForbidden());
    }
}
