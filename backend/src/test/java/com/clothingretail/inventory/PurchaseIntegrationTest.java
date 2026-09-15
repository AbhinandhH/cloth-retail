package com.clothingretail.inventory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.clothingretail.masterdata.Vendor;
import com.clothingretail.masterdata.repository.VendorRepository;
import com.clothingretail.product.ProductVariant;
import com.clothingretail.product.repository.ProductVariantRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Verifies that creating a purchase through the admin API increments the
 * target ProductVariant's live stockQuantity and appends a PURCHASE_IN
 * InventoryTransaction row - the two things that must never drift apart
 * (see PurchaseService).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PurchaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private ProductVariantRepository productVariantRepository;

    @Autowired
    private VendorRepository vendorRepository;

    @Autowired
    private InventoryTransactionRepository inventoryTransactionRepository;

    private String adminAccessToken() throws Exception {
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

    @Test
    void creatingAPurchaseIncreasesStockAndWritesInventoryTransaction() throws Exception {
        ProductVariant variant = productVariantRepository.findBySku("FSK-BLU-M").orElseThrow();
        Vendor vendor = vendorRepository.findAll().stream().findFirst().orElseThrow();
        int stockBefore = variant.getStockQuantity();
        int purchaseQuantity = 7;

        String token = adminAccessToken();
        String purchaseBody = """
                {"vendorId":%d,"invoiceNumber":"INV-TEST-001","items":[{"productVariantId":%d,"quantity":%d,"purchasePrice":450.00}]}
                """.formatted(vendor.getId(), variant.getId(), purchaseQuantity);

        MvcResult result = mockMvc.perform(post("/api/admin/purchases")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(purchaseBody))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        Long purchaseId = json.get("id").asLong();
        assertThat(json.get("items")).hasSize(1);

        ProductVariant variantAfter = productVariantRepository.findById(variant.getId()).orElseThrow();
        assertThat(variantAfter.getStockQuantity()).isEqualTo(stockBefore + purchaseQuantity);

        List<InventoryTransaction> transactions =
                inventoryTransactionRepository.findByReferenceTypeAndReferenceId("PURCHASE", purchaseId);
        assertThat(transactions).hasSize(1);
        assertThat(transactions.get(0).getType()).isEqualTo(InventoryTransactionType.PURCHASE_IN);
        assertThat(transactions.get(0).getQuantity()).isEqualTo(purchaseQuantity);
        assertThat(transactions.get(0).getProductVariant().getId()).isEqualTo(variant.getId());
    }

    @Test
    void nonAdminCannotCreatePurchase() throws Exception {
        // A CUSTOMER access token (no admin role at all) must be rejected by @PreAuthorize.
        String registerBody = """
                {"fullName":"Not Admin","email":"not-admin-purchase-%s@example.com","mobileNumber":"9000000000","password":"Password123!"}
                """.formatted(System.nanoTime());
        MvcResult registerResult = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode registerJson = objectMapper.readTree(registerResult.getResponse().getContentAsString());
        String customerToken = registerJson.get("auth").get("accessToken").asText();

        ProductVariant variant = productVariantRepository.findBySku("FSK-BLU-M").orElseThrow();
        Vendor vendor = vendorRepository.findAll().stream().findFirst().orElseThrow();
        String purchaseBody = """
                {"vendorId":%d,"items":[{"productVariantId":%d,"quantity":1,"purchasePrice":100.00}]}
                """.formatted(vendor.getId(), variant.getId());

        mockMvc.perform(post("/api/admin/purchases")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(purchaseBody))
                .andExpect(status().isForbidden());
    }
}
