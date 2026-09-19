package com.clothingretail.inventory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.clothingretail.product.ProductVariant;
import com.clothingretail.product.repository.ProductVariantRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Smoke-tests the read side of the admin inventory module (GET /variants with
 * filters/sort, /dashboard, /transactions, /damages, /variants/{id}/transactions) - the parts
 * that exercise Criteria-based computed-expression predicates and nested-property sort, not
 * otherwise covered by StockAdjustmentIntegrationTest / DamageIntegrationTest.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class InventoryQueryIntegrationTest {

    @Autowired
    private org.springframework.test.web.servlet.MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private ProductVariantRepository productVariantRepository;

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

    @Test
    void listVariantsSupportsFiltersAndSort() throws Exception {
        String token = adminAccessToken();

        MvcResult result = mockMvc.perform(get("/api/admin/inventory/variants")
                        .header("Authorization", "Bearer " + token)
                        .param("page", "0")
                        .param("size", "5")
                        .param("sort", "name")
                        .param("dir", "asc"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(body.get("content")).isNotEmpty();
        JsonNode firstRow = body.get("content").get(0);
        assertThat(firstRow.has("variantId")).isTrue();
        assertThat(firstRow.has("availableQuantity")).isTrue();
        assertThat(firstRow.has("stockStatus")).isTrue();

        mockMvc.perform(get("/api/admin/inventory/variants")
                        .header("Authorization", "Bearer " + token)
                        .param("stockStatus", "IN_STOCK")
                        .param("sort", "stock")
                        .param("dir", "desc"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/admin/inventory/variants")
                        .header("Authorization", "Bearer " + token)
                        .param("stockStatus", "OUT_OF_STOCK"))
                .andExpect(status().isOk());
    }

    @Test
    void dashboardReturnsAggregateShape() throws Exception {
        String token = adminAccessToken();

        MvcResult result = mockMvc.perform(get("/api/admin/inventory/dashboard")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(body.get("totalProducts").asLong()).isGreaterThan(0);
        assertThat(body.get("totalVariants").asLong()).isGreaterThan(0);
        assertThat(body.has("lowStockItems")).isTrue();
        assertThat(body.has("outOfStockItems")).isTrue();
        assertThat(body.has("recentProducts")).isTrue();
        assertThat(body.has("recentTransactions")).isTrue();
        assertThat(body.has("recentDamages")).isTrue();
    }

    @Test
    void transactionsAndDamagesListingsAreReachable() throws Exception {
        String token = adminAccessToken();
        ProductVariant variant = productVariantRepository.findBySku("CSD-WHT-M").orElseThrow();

        // Generate at least one transaction and one damage record for this variant so the
        // variant-scoped listing has content to return.
        mockMvc.perform(post("/api/admin/inventory/variants/" + variant.getId() + "/adjust")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"quantityChange":1,"reason":"smoke test"}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/admin/inventory/transactions")
                        .header("Authorization", "Bearer " + token)
                        .param("variantId", String.valueOf(variant.getId())))
                .andExpect(status().isOk());

        MvcResult variantTxResult = mockMvc.perform(get("/api/admin/inventory/variants/" + variant.getId() + "/transactions")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode txBody = objectMapper.readTree(variantTxResult.getResponse().getContentAsString());
        assertThat(txBody.get("content")).isNotEmpty();

        mockMvc.perform(get("/api/admin/inventory/damages")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }
}
