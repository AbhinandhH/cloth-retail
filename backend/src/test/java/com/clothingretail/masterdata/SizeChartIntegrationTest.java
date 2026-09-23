package com.clothingretail.masterdata;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.clothingretail.masterdata.repository.CategoryRepository;
import com.clothingretail.masterdata.repository.ColorRepository;
import com.clothingretail.masterdata.repository.MaterialRepository;
import com.clothingretail.masterdata.repository.SizeRepository;
import com.clothingretail.masterdata.repository.VendorRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Covers the Size Chart master (CRUD, the columns/rows length-match validation, and the
 * delete-guard once a product references one) plus the public product-detail endpoint
 * exposing an assigned chart's columns/rows to customers.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SizeChartIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private MaterialRepository materialRepository;

    @Autowired
    private SizeRepository sizeRepository;

    @Autowired
    private ColorRepository colorRepository;

    @Autowired
    private VendorRepository vendorRepository;

    /** SUPER_ADMIN (the software owner) now has every ADMIN privilege too (see SecurityConfig's RoleHierarchy bean), but tests still exercise a plain store ADMIN account here - more representative of real usage than the platform-owner bootstrap account. */
    private String adminAccessToken() throws Exception {
        String superAdminLoginBody = """
                {"email":"admin@clothingretail.local","password":"ChangeMe123!"}
                """;
        MvcResult superAdminResult = mockMvc.perform(post("/api/admin/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(superAdminLoginBody))
                .andExpect(status().isOk())
                .andReturn();
        String superAdminToken =
                objectMapper.readTree(superAdminResult.getResponse().getContentAsString()).get("accessToken").asText();

        String email = "test-admin-" + System.nanoTime() + "@example.com";
        String createBody = """
                {"fullName":"Test Admin","email":"%s","password":"Password123!","role":"ADMIN"}
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
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("accessToken").asText();
    }

    private String unique() {
        return String.valueOf(System.nanoTime());
    }

    @Test
    void createGetUpdateAndDeleteRoundTrip() throws Exception {
        String token = adminAccessToken();
        String name = "Tops - Standard " + unique();
        String createBody = """
                {"name":"%s","description":"Regular fit tops","displayOrder":1,"active":true,
                 "columns":["Chest","Waist"],
                 "rows":[{"sizeLabel":"S","values":["34-36","28-30"]},{"sizeLabel":"M","values":["38-40","32-34"]}]}
                """.formatted(name);

        MvcResult createResult = mockMvc.perform(post("/api/admin/size-charts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(name))
                .andExpect(jsonPath("$.columns[0]").value("Chest"))
                .andExpect(jsonPath("$.columns[1]").value("Waist"))
                .andExpect(jsonPath("$.rows[0].sizeLabel").value("S"))
                .andExpect(jsonPath("$.rows[0].values[0]").value("34-36"))
                .andExpect(jsonPath("$.rows[1].sizeLabel").value("M"))
                .andReturn();
        long id = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(get("/api/admin/size-charts/" + id).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rows.length()").value(2));

        String updateBody = """
                {"name":"%s","description":"Regular fit tops","displayOrder":1,"active":true,
                 "columns":["Chest","Waist","Length"],
                 "rows":[{"sizeLabel":"S","values":["34-36","28-30","27"]}]}
                """.formatted(name);
        mockMvc.perform(put("/api/admin/size-charts/" + id)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.columns.length()").value(3))
                .andExpect(jsonPath("$.rows.length()").value(1))
                .andExpect(jsonPath("$.rows[0].values[2]").value("27"));

        mockMvc.perform(delete("/api/admin/size-charts/" + id).header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
    }

    @Test
    void createRejectsARowWhoseValueCountDoesNotMatchColumnCount() throws Exception {
        String token = adminAccessToken();
        String createBody = """
                {"name":"Mismatched %s","displayOrder":1,"active":true,
                 "columns":["Chest","Waist"],
                 "rows":[{"sizeLabel":"S","values":["34-36"]}]}
                """.formatted(unique());

        mockMvc.perform(post("/api/admin/size-charts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createRejectsDuplicateName() throws Exception {
        String token = adminAccessToken();
        String name = "Duplicate Chart " + unique();
        String createBody = """
                {"name":"%s","displayOrder":1,"active":true,"columns":["Chest"],"rows":[]}
                """.formatted(name);

        mockMvc.perform(post("/api/admin/size-charts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/admin/size-charts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isConflict());
    }

    @Test
    void deleteRejectedWhenReferencedByAProductButSucceedsOnceUnreferenced() throws Exception {
        String token = adminAccessToken();
        String suffix = unique();

        String chartBody = """
                {"name":"Footwear %s","displayOrder":1,"active":true,
                 "columns":["Foot length (cm)"],"rows":[{"sizeLabel":"9","values":["27"]}]}
                """.formatted(suffix);
        MvcResult chartResult = mockMvc.perform(post("/api/admin/size-charts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(chartBody))
                .andExpect(status().isOk())
                .andReturn();
        long chartId = objectMapper.readTree(chartResult.getResponse().getContentAsString()).get("id").asLong();

        Long categoryId = categoryRepository.findAll().stream().findFirst().orElseThrow().getId();
        Long materialId = materialRepository.findAll().stream().findFirst().orElseThrow().getId();
        Long sizeId = sizeRepository.findAll().stream().findFirst().orElseThrow().getId();
        Long colorId = colorRepository.findAll().stream().findFirst().orElseThrow().getId();
        Long vendorId = vendorRepository.findAll().stream().findFirst().orElseThrow().getId();
        String productBody = """
                {"categoryId":%d,"materialId":%d,"vendorId":%d,"sizeChartId":%d,
                 "name":"Size Chart Test Product %s","slug":"size-chart-test-%s","status":"ACTIVE",
                 "variants":[{"sku":"SCT-%s","sizeId":%d,"colorId":%d,"sellingPrice":499.00,"stockQuantity":5}]}
                """.formatted(categoryId, materialId, vendorId, chartId, suffix, suffix, suffix, sizeId, colorId);
        mockMvc.perform(post("/api/admin/products")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sizeChartId").value(chartId))
                .andExpect(jsonPath("$.sizeChartName").value("Footwear " + suffix));

        MvcResult conflictResult = mockMvc.perform(delete("/api/admin/size-charts/" + chartId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isConflict())
                .andReturn();
        JsonNode conflictJson = objectMapper.readTree(conflictResult.getResponse().getContentAsString());
        assertThat(conflictJson.get("message").asText()).contains("1").contains("products");

        mockMvc.perform(get("/api/products/size-chart-test-" + suffix))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sizeChart.name").value("Footwear " + suffix))
                .andExpect(jsonPath("$.sizeChart.columns[0]").value("Foot length (cm)"))
                .andExpect(jsonPath("$.sizeChart.rows[0].sizeLabel").value("9"))
                .andExpect(jsonPath("$.sizeChart.rows[0].values[0]").value("27"));

        String unusedChartBody = """
                {"name":"Unused Chart %s","displayOrder":1,"active":true,"columns":["Chest"],"rows":[]}
                """.formatted(suffix);
        MvcResult unusedChartResult = mockMvc.perform(post("/api/admin/size-charts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(unusedChartBody))
                .andExpect(status().isOk())
                .andReturn();
        long unusedChartId = objectMapper.readTree(unusedChartResult.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(delete("/api/admin/size-charts/" + unusedChartId).header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
    }

    @Test
    void productWithNoSizeChartExposesNullOnPublicDetail() throws Exception {
        String token = adminAccessToken();
        String suffix = unique();
        Long categoryId = categoryRepository.findAll().stream().findFirst().orElseThrow().getId();
        Long materialId = materialRepository.findAll().stream().findFirst().orElseThrow().getId();
        Long sizeId = sizeRepository.findAll().stream().findFirst().orElseThrow().getId();
        Long colorId = colorRepository.findAll().stream().findFirst().orElseThrow().getId();
        Long vendorId = vendorRepository.findAll().stream().findFirst().orElseThrow().getId();
        String productBody = """
                {"categoryId":%d,"materialId":%d,"vendorId":%d,
                 "name":"No Chart Product %s","slug":"no-chart-product-%s","status":"ACTIVE",
                 "variants":[{"sku":"NCP-%s","sizeId":%d,"colorId":%d,"sellingPrice":499.00,"stockQuantity":5}]}
                """.formatted(categoryId, materialId, vendorId, suffix, suffix, suffix, sizeId, colorId);
        mockMvc.perform(post("/api/admin/products")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sizeChartId").doesNotExist());

        mockMvc.perform(get("/api/products/no-chart-product-" + suffix))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sizeChart").doesNotExist());
    }
}
