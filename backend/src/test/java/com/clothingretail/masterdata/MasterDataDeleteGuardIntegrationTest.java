package com.clothingretail.masterdata;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.clothingretail.masterdata.repository.CategoryRepository;
import com.clothingretail.masterdata.repository.SubCategoryRepository;
import com.clothingretail.product.repository.ProductRepository;
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
 * Verifies the delete-guards added to the 7 existing master-data services: deleting a
 * category/sub-category/size/color/brand/material/vendor that's still referenced by a
 * Product/ProductVariant/SubCategory/Purchase is rejected with 409 and a count-bearing
 * message; deleting an unreferenced one succeeds. Uses V3's seeded demo data for the
 * "referenced" cases (guaranteed present regardless of other tests' run order) and creates
 * fresh master rows via the admin API for the "unreferenced" cases.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MasterDataDeleteGuardIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private SubCategoryRepository subCategoryRepository;

    @Autowired
    private ProductRepository productRepository;

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

    private long createAndGetId(String token, String path, String body) throws Exception {
        MvcResult result = mockMvc.perform(post(path)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    // ── Category ─────────────────────────────────────────────────────────────

    @Test
    void categoryDeleteRejectedWhenReferencedByProducts() throws Exception {
        Category kurtis = categoryRepository.findBySlug("kurtis").orElseThrow();
        // Don't hardcode the count: other test classes sharing this DB may add/remove products
        // under "kurtis" depending on run order - what matters is the guard fires with the
        // *actual* count, not a specific number.
        long expectedCount = productRepository.countByCategoryId(kurtis.getId());
        assertThat(expectedCount).isGreaterThan(0);
        String token = adminAccessToken();

        MvcResult result = mockMvc.perform(delete("/api/admin/categories/" + kurtis.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isConflict())
                .andReturn();
        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(json.get("message").asText()).contains(String.valueOf(expectedCount)).contains("products");
    }

    @Test
    void categoryDeleteRejectedWhenItHasSubCategories() throws Exception {
        // "bottoms" has zero products in the V3 seed but sub-categories (jeans, trousers) -
        // exercises the OTHER guard branch on Category (SubCategory parentage), not the Product
        // one. Read the actual count first: subCategoryDeleteSucceedsWhenUnreferenced (this same
        // class) deletes "jeans" in its own test, and test method order isn't guaranteed.
        Category bottoms = categoryRepository.findBySlug("bottoms").orElseThrow();
        long expectedCount = subCategoryRepository.countByCategoryId(bottoms.getId());
        assertThat(expectedCount).isGreaterThan(0);
        String token = adminAccessToken();

        MvcResult result = mockMvc.perform(delete("/api/admin/categories/" + bottoms.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isConflict())
                .andReturn();
        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(json.get("message").asText()).contains(String.valueOf(expectedCount)).contains("sub-categories");
    }

    @Test
    void categoryDeleteSucceedsWhenUnreferenced() throws Exception {
        String token = adminAccessToken();
        String suffix = unique();
        String createBody = """
                {"name":"Guard Test Category %s","slug":"guard-test-category-%s","displayOrder":99,"active":true}
                """.formatted(suffix, suffix);
        long id = createAndGetId(token, "/api/admin/categories", createBody);

        mockMvc.perform(delete("/api/admin/categories/" + id).header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
    }

    // ── SubCategory ──────────────────────────────────────────────────────────

    @Test
    void subCategoryDeleteRejectedWhenReferencedByProducts() throws Exception {
        SubCategory straightKurtis = subCategoryRepository.findAll().stream()
                .filter(sc -> "straight-kurtis".equals(sc.getSlug()))
                .findFirst()
                .orElseThrow();
        String token = adminAccessToken();

        MvcResult result = mockMvc.perform(delete("/api/admin/sub-categories/" + straightKurtis.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isConflict())
                .andReturn();
        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(json.get("message").asText()).contains("1").contains("products");
    }

    @Test
    void subCategoryDeleteSucceedsWhenUnreferenced() throws Exception {
        // "jeans" (under bottoms) has no products in the V3 seed.
        SubCategory jeans =
                subCategoryRepository.findAll().stream().filter(sc -> "jeans".equals(sc.getSlug())).findFirst().orElseThrow();
        String token = adminAccessToken();

        mockMvc.perform(delete("/api/admin/sub-categories/" + jeans.getId()).header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
    }

    // ── Size ─────────────────────────────────────────────────────────────────

    @Test
    void sizeDeleteRejectedWhenReferencedByVariants() throws Exception {
        String token = adminAccessToken();
        long mediumSizeId = findAdminIdByName(token, "/api/admin/sizes", "M");

        MvcResult result = mockMvc.perform(delete("/api/admin/sizes/" + mediumSizeId).header("Authorization", "Bearer " + token))
                .andExpect(status().isConflict())
                .andReturn();
        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(json.get("message").asText()).contains("product variants");
    }

    @Test
    void sizeDeleteSucceedsWhenUnreferenced() throws Exception {
        String token = adminAccessToken();
        String createBody = """
                {"name":"XXL-%s","displayOrder":99,"active":true}
                """.formatted(unique());
        long id = createAndGetId(token, "/api/admin/sizes", createBody);

        mockMvc.perform(delete("/api/admin/sizes/" + id).header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
    }

    // ── Color ────────────────────────────────────────────────────────────────

    @Test
    void colorDeleteRejectedWhenReferencedByVariants() throws Exception {
        String token = adminAccessToken();
        long redId = findAdminIdByName(token, "/api/admin/colors", "Red");

        MvcResult result = mockMvc.perform(delete("/api/admin/colors/" + redId).header("Authorization", "Bearer " + token))
                .andExpect(status().isConflict())
                .andReturn();
        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(json.get("message").asText()).contains("product variants");
    }

    @Test
    void colorDeleteSucceedsWhenUnreferenced() throws Exception {
        String token = adminAccessToken();
        String createBody = """
                {"name":"Guard Test Color %s","hexCode":"#123456","displayOrder":99,"active":true}
                """.formatted(unique());
        long id = createAndGetId(token, "/api/admin/colors", createBody);

        mockMvc.perform(delete("/api/admin/colors/" + id).header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
    }

    // ── Brand ────────────────────────────────────────────────────────────────

    @Test
    void brandDeleteRejectedWhenReferencedByProducts() throws Exception {
        String token = adminAccessToken();
        long brandId = findAdminIdByName(token, "/api/admin/brands", "Urban Thread");

        MvcResult result = mockMvc.perform(delete("/api/admin/brands/" + brandId).header("Authorization", "Bearer " + token))
                .andExpect(status().isConflict())
                .andReturn();
        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(json.get("message").asText()).contains("products");
    }

    @Test
    void brandDeleteSucceedsWhenUnreferenced() throws Exception {
        String token = adminAccessToken();
        String createBody = """
                {"name":"Guard Test Brand %s","displayOrder":99,"active":true}
                """.formatted(unique());
        long id = createAndGetId(token, "/api/admin/brands", createBody);

        mockMvc.perform(delete("/api/admin/brands/" + id).header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
    }

    // ── Material ─────────────────────────────────────────────────────────────

    @Test
    void materialDeleteRejectedWhenReferencedByProducts() throws Exception {
        String token = adminAccessToken();
        long materialId = findAdminIdByName(token, "/api/admin/materials", "Cotton");

        MvcResult result = mockMvc.perform(delete("/api/admin/materials/" + materialId).header("Authorization", "Bearer " + token))
                .andExpect(status().isConflict())
                .andReturn();
        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(json.get("message").asText()).contains("products");
    }

    @Test
    void materialDeleteSucceedsWhenUnreferenced() throws Exception {
        String token = adminAccessToken();
        String createBody = """
                {"name":"Guard Test Material %s","displayOrder":99,"active":true}
                """.formatted(unique());
        long id = createAndGetId(token, "/api/admin/materials", createBody);

        mockMvc.perform(delete("/api/admin/materials/" + id).header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
    }

    // ── Vendor ───────────────────────────────────────────────────────────────

    @Test
    void vendorDeleteRejectedWhenReferencedByPurchases() throws Exception {
        String token = adminAccessToken();
        String suffix = unique();
        String vendorBody = """
                {"name":"Guard Test Vendor %s","active":true}
                """.formatted(suffix);
        long vendorId = createAndGetId(token, "/api/admin/vendors", vendorBody);

        String purchaseBody = """
                {"vendorId":%d,"invoiceNumber":"GUARD-TEST-%s","items":[{"productVariantId":%d,"quantity":1,"purchasePrice":100.00}]}
                """.formatted(vendorId, suffix, variantIdBySku("FSK-BLU-M"));
        mockMvc.perform(post("/api/admin/purchases")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(purchaseBody))
                .andExpect(status().isOk());

        MvcResult result = mockMvc.perform(delete("/api/admin/vendors/" + vendorId).header("Authorization", "Bearer " + token))
                .andExpect(status().isConflict())
                .andReturn();
        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(json.get("message").asText()).contains("1").contains("purchases");
    }

    @Test
    void vendorDeleteSucceedsWhenUnreferenced() throws Exception {
        String token = adminAccessToken();
        String createBody = """
                {"name":"Guard Test Vendor Unused %s","active":true}
                """.formatted(unique());
        long id = createAndGetId(token, "/api/admin/vendors", createBody);

        mockMvc.perform(delete("/api/admin/vendors/" + id).header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private long findAdminIdByName(String token, String basePath, String name) throws Exception {
        MvcResult result = mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(basePath)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode array = objectMapper.readTree(result.getResponse().getContentAsString());
        for (JsonNode node : array) {
            if (name.equals(node.get("name").asText())) {
                return node.get("id").asLong();
            }
        }
        throw new IllegalStateException("No entry named " + name + " found at " + basePath);
    }

    private long variantIdBySku(String sku) throws Exception {
        String token = adminAccessToken();
        MvcResult result = mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(
                                "/api/admin/inventory/variants")
                        .header("Authorization", "Bearer " + token)
                        .param("q", sku)
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode content = objectMapper.readTree(result.getResponse().getContentAsString()).get("content");
        for (JsonNode node : content) {
            if (sku.equals(node.get("sku").asText())) {
                return node.get("variantId").asLong();
            }
        }
        throw new IllegalStateException("No variant with SKU " + sku);
    }
}
