package com.clothingretail.inventory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.clothingretail.inventory.repository.DamageReasonRepository;
import com.clothingretail.inventory.repository.DamageRecordRepository;
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
 * Verifies POST /api/admin/inventory/variants/{id}/damage: marking damage increments
 * damagedQuantity (reducing computed availableQuantity) WITHOUT touching the physical
 * stockQuantity, and writes both a DamageRecord and a DAMAGE InventoryTransaction
 * (see StockService.recordDamage).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DamageIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private ProductVariantRepository productVariantRepository;

    @Autowired
    private InventoryTransactionRepository inventoryTransactionRepository;

    @Autowired
    private DamageRecordRepository damageRecordRepository;

    @Autowired
    private DamageReasonRepository damageReasonRepository;

    private Long reasonIdByCode(String code) {
        return damageReasonRepository.findAll().stream()
                .filter(r -> code.equals(r.getCode()))
                .findFirst()
                .orElseThrow()
                .getId();
    }

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

    @Test
    void damageReducesAvailableQuantityButNotStockQuantity() throws Exception {
        ProductVariant variant = productVariantRepository.findBySku("MCS-BLU-M").orElseThrow();
        int stockBefore = variant.getStockQuantity();
        int damagedBefore = variant.getDamagedQuantity();
        int availableBefore = variant.getAvailableQuantity();

        String body = """
                {"quantity":3,"reasonId":%d,"notes":"Torn during shipping"}
                """.formatted(reasonIdByCode("TRANSIT_DAMAGE"));
        MvcResult result = mockMvc.perform(post("/api/admin/inventory/variants/" + variant.getId() + "/damage")
                        .header("Authorization", "Bearer " + adminAccessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(json.get("damagedQuantity").asInt()).isEqualTo(damagedBefore + 3);
        assertThat(json.get("availableQuantity").asInt()).isEqualTo(availableBefore - 3);

        ProductVariant after = productVariantRepository.findById(variant.getId()).orElseThrow();
        assertThat(after.getStockQuantity()).isEqualTo(stockBefore);
        assertThat(after.getDamagedQuantity()).isEqualTo(damagedBefore + 3);
        assertThat(after.getAvailableQuantity()).isEqualTo(availableBefore - 3);
    }

    @Test
    void damageWritesDamageRecordAndInventoryTransaction() throws Exception {
        ProductVariant variant = productVariantRepository.findBySku("MCS-BLU-L").orElseThrow();
        int damagedBefore = variant.getDamagedQuantity();

        String body = """
                {"quantity":2,"reasonId":%d,"notes":"Water damage on shelf"}
                """.formatted(reasonIdByCode("WAREHOUSE_DAMAGE"));
        mockMvc.perform(post("/api/admin/inventory/variants/" + variant.getId() + "/damage")
                        .header("Authorization", "Bearer " + adminAccessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        List<DamageRecord> damageRecords = damageRecordRepository.findAll().stream()
                .filter(d -> d.getProductVariant().getId().equals(variant.getId()))
                .toList();
        assertThat(damageRecords).isNotEmpty();
        DamageRecord record = damageRecords.get(damageRecords.size() - 1);
        assertThat(record.getQuantity()).isEqualTo(2);
        assertThat(record.getReason().getCode()).isEqualTo("WAREHOUSE_DAMAGE");
        assertThat(record.getNotes()).isEqualTo("Water damage on shelf");

        List<InventoryTransaction> transactions = inventoryTransactionRepository.findByProductVariantId(variant.getId());
        InventoryTransaction last = transactions.get(transactions.size() - 1);
        assertThat(last.getType()).isEqualTo(InventoryTransactionType.DAMAGE);
        assertThat(last.getQuantity()).isEqualTo(2);
        assertThat(last.getPreviousQuantity()).isEqualTo(damagedBefore);
        assertThat(last.getNewQuantity()).isEqualTo(damagedBefore + 2);
    }

    @Test
    void damageRejectsQuantityExceedingAvailable() throws Exception {
        ProductVariant variant = productVariantRepository.findBySku("MCS-GRN-XL").orElseThrow();
        int available = variant.getAvailableQuantity();

        String body = """
                {"quantity":%d,"reasonId":%d,"notes":"Too much"}
                """.formatted(available + 1, reasonIdByCode("OTHER"));
        mockMvc.perform(post("/api/admin/inventory/variants/" + variant.getId() + "/damage")
                        .header("Authorization", "Bearer " + adminAccessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());

        ProductVariant after = productVariantRepository.findById(variant.getId()).orElseThrow();
        assertThat(after.getAvailableQuantity()).isEqualTo(available);
    }
}
