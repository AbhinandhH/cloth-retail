package com.clothingretail.inventory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.clothingretail.inventory.repository.DamageReasonRepository;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Covers the DamageReason master-data admin CRUD + delete-guard (converted from a fixed enum
 * in this pass - see DamageReason's Javadoc), the public /api/damage-reasons lookup, and that
 * recording damage end-to-end with the new reasonId-based request still works and surfaces the
 * *resolved* reason name (not a raw code) in both the damages listing and the variant
 * transaction history - see StockService#recordDamage and InventoryQueryService#toRow.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DamageReasonIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private DamageReasonRepository damageReasonRepository;

    @Autowired
    private ProductVariantRepository productVariantRepository;

    private String adminAccessToken() throws Exception {
        String loginBody = """
                {"email":"admin@clothingretail.local","password":"ChangeMe123!"}
                """;
        MvcResult result = mockMvc.perform(post("/api/admin/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("accessToken").asText();
    }

    @Test
    void publicListingReturnsOnlyActiveSeededReasons() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/damage-reasons")).andExpect(status().isOk()).andReturn();
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(body).hasSizeGreaterThanOrEqualTo(5);
        boolean hasDefective = false;
        for (JsonNode node : body) {
            if ("Defective".equals(node.get("name").asText())) {
                hasDefective = true;
            }
        }
        assertThat(hasDefective).isTrue();
    }

    @Test
    void adminCrudAndDeleteGuardWork() throws Exception {
        String token = adminAccessToken();
        String suffix = String.valueOf(System.nanoTime());

        String createBody = """
                {"name":"Guard Test Reason %s","code":"GUARD_TEST_%s","description":"test","displayOrder":50,"active":true}
                """.formatted(suffix, suffix);
        MvcResult createResult = mockMvc.perform(post("/api/admin/damage-reasons")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isOk())
                .andReturn();
        long reasonId =
                objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asLong();

        // Unreferenced -> delete succeeds.
        mockMvc.perform(delete("/api/admin/damage-reasons/" + reasonId).header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        // A seeded reason that damage records already reference (via the test below and/or V6's
        // migration comment) cannot be deleted - use "OTHER", creating one damage record against
        // it here so this test is self-contained regardless of run order.
        long otherReasonId = reasonIdByCode("OTHER");
        ProductVariant variant = productVariantRepository.findBySku("AEK-GRN-M").orElseThrow();
        String damageBody = """
                {"quantity":1,"reasonId":%d,"notes":"guard setup"}
                """.formatted(otherReasonId);
        mockMvc.perform(post("/api/admin/inventory/variants/" + variant.getId() + "/damage")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(damageBody))
                .andExpect(status().isOk());

        MvcResult deleteResult = mockMvc.perform(delete("/api/admin/damage-reasons/" + otherReasonId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isConflict())
                .andReturn();
        JsonNode json = objectMapper.readTree(deleteResult.getResponse().getContentAsString());
        assertThat(json.get("message").asText()).contains("damage record");
    }

    @Test
    void damageHistoryAndDamagesListingShowResolvedReasonNameNotRawCode() throws Exception {
        String token = adminAccessToken();
        ProductVariant variant = productVariantRepository.findBySku("MFW-WHT-M").orElseThrow();
        long reasonId = reasonIdByCode("RETURN_DAMAGE");

        String damageBody = """
                {"quantity":1,"reasonId":%d,"notes":"resolved name check"}
                """.formatted(reasonId);
        mockMvc.perform(post("/api/admin/inventory/variants/" + variant.getId() + "/damage")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(damageBody))
                .andExpect(status().isOk());

        MvcResult damagesResult = mockMvc.perform(get("/api/admin/inventory/damages")
                        .header("Authorization", "Bearer " + token)
                        .param("variantId", String.valueOf(variant.getId())))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode damagesContent = objectMapper.readTree(damagesResult.getResponse().getContentAsString()).get("content");
        assertThat(damagesContent).isNotEmpty();
        JsonNode lastDamage = damagesContent.get(0);
        assertThat(lastDamage.get("reasonId").asLong()).isEqualTo(reasonId);
        assertThat(lastDamage.get("reasonName").asText()).isEqualTo("Return Damage");

        MvcResult transactionsResult = mockMvc.perform(get("/api/admin/inventory/variants/" + variant.getId() + "/transactions")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode txContent = objectMapper.readTree(transactionsResult.getResponse().getContentAsString()).get("content");
        JsonNode damageTx = null;
        for (JsonNode node : txContent) {
            if ("DAMAGE".equals(node.get("type").asText())) {
                damageTx = node;
                break;
            }
        }
        assertThat(damageTx).isNotNull();
        // The transaction row's free-text `reason` field must hold the resolved reason name
        // ("Return Damage"), not the old enum constant ("RETURN_DAMAGE").
        assertThat(damageTx.get("reason").asText()).isEqualTo("Return Damage");
    }

    private long reasonIdByCode(String code) {
        return damageReasonRepository.findAll().stream()
                .filter(r -> code.equals(r.getCode()))
                .findFirst()
                .orElseThrow()
                .getId();
    }
}
