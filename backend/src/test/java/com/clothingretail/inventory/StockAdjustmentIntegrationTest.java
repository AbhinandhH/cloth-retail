package com.clothingretail.inventory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.clothingretail.inventory.repository.InventoryTransactionRepository;
import com.clothingretail.product.ProductVariant;
import com.clothingretail.product.repository.ProductVariantRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Verifies POST /api/admin/inventory/variants/{id}/adjust: it mutates
 * ProductVariant.stockQuantity and appends an ADJUSTMENT InventoryTransaction with a correct
 * previous/new snapshot - the same "never drift apart" guarantee PurchaseIntegrationTest checks
 * for purchases (see StockService.adjust).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class StockAdjustmentIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private ProductVariantRepository productVariantRepository;

    @Autowired
    private InventoryTransactionRepository inventoryTransactionRepository;

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
        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return json.get("accessToken").asText();
    }

    private String customerAccessToken() throws Exception {
        String registerBody = """
                {"fullName":"Not Admin","email":"not-admin-stock-%s@example.com","mobileNumber":"9000000000","password":"Password123!"}
                """.formatted(System.nanoTime());
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("auth").get("accessToken").asText();
    }

    @Test
    void adjustIncreasesStockAndWritesTransactionWithSnapshot() throws Exception {
        ProductVariant variant = productVariantRepository.findBySku("AEK-GRN-S").orElseThrow();
        int stockBefore = variant.getStockQuantity();

        String body = """
                {"quantityChange":5,"reason":"Recount correction"}
                """;
        MvcResult result = mockMvc.perform(post("/api/admin/inventory/variants/" + variant.getId() + "/adjust")
                        .header("Authorization", "Bearer " + adminAccessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(json.get("previousQuantity").asInt()).isEqualTo(stockBefore);
        assertThat(json.get("newQuantity").asInt()).isEqualTo(stockBefore + 5);
        assertThat(json.get("availableQuantity").asInt()).isEqualTo(stockBefore + 5);

        ProductVariant after = productVariantRepository.findById(variant.getId()).orElseThrow();
        assertThat(after.getStockQuantity()).isEqualTo(stockBefore + 5);

        List<InventoryTransaction> transactions = inventoryTransactionRepository.findByProductVariantId(variant.getId());
        InventoryTransaction last = transactions.get(transactions.size() - 1);
        assertThat(last.getType()).isEqualTo(InventoryTransactionType.ADJUSTMENT);
        assertThat(last.getPreviousQuantity()).isEqualTo(stockBefore);
        assertThat(last.getNewQuantity()).isEqualTo(stockBefore + 5);
        assertThat(last.getQuantity()).isEqualTo(5);
        assertThat(last.getReason()).isEqualTo("Recount correction");
    }

    @Test
    void adjustDecreasesStockCorrectly() throws Exception {
        ProductVariant variant = productVariantRepository.findBySku("AEK-GRN-M").orElseThrow();
        int stockBefore = variant.getStockQuantity();

        String body = """
                {"quantityChange":-3,"reason":"Damaged in warehouse recount"}
                """;
        mockMvc.perform(post("/api/admin/inventory/variants/" + variant.getId() + "/adjust")
                        .header("Authorization", "Bearer " + adminAccessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath(
                        "$.newQuantity", org.hamcrest.Matchers.is(stockBefore - 3)));

        ProductVariant after = productVariantRepository.findById(variant.getId()).orElseThrow();
        assertThat(after.getStockQuantity()).isEqualTo(stockBefore - 3);
    }

    @Test
    void adjustRejectsResultGoingNegative() throws Exception {
        ProductVariant variant = productVariantRepository.findBySku("ALP-WHT-M").orElseThrow();
        int stockBefore = variant.getStockQuantity();

        String body = """
                {"quantityChange":%d,"reason":"Should fail"}
                """.formatted(-(stockBefore + 1));
        mockMvc.perform(post("/api/admin/inventory/variants/" + variant.getId() + "/adjust")
                        .header("Authorization", "Bearer " + adminAccessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());

        ProductVariant after = productVariantRepository.findById(variant.getId()).orElseThrow();
        assertThat(after.getStockQuantity()).isEqualTo(stockBefore);
    }

    @Test
    void unauthenticatedRequestIsRejected() throws Exception {
        ProductVariant variant = productVariantRepository.findBySku("ALP-WHT-L").orElseThrow();
        String body = """
                {"quantityChange":1,"reason":"test"}
                """;
        mockMvc.perform(post("/api/admin/inventory/variants/" + variant.getId() + "/adjust")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void plainCustomerCannotAdjustStock() throws Exception {
        ProductVariant variant = productVariantRepository.findBySku("ALP-WHT-L").orElseThrow();
        String body = """
                {"quantityChange":1,"reason":"test"}
                """;
        mockMvc.perform(post("/api/admin/inventory/variants/" + variant.getId() + "/adjust")
                        .header("Authorization", "Bearer " + customerAccessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
    }
}
