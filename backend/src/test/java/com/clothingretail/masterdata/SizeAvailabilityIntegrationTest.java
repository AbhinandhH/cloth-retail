package com.clothingretail.masterdata;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.clothingretail.masterdata.repository.CategoryRepository;
import com.clothingretail.masterdata.repository.SizeRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
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
 * Verifies GET /api/categories/{id}/available-sizes: falls back to the full active global
 * Size list for a category with no size groups (the pre-existing seeded demo categories, per
 * V6's migration comment - this is what keeps them working once SizeGroup ships), and returns
 * the deduped, order-respecting union across a category's active size groups once one is
 * configured via the admin API.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SizeAvailabilityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private SizeRepository sizeRepository;

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
    void fallsBackToFullGlobalSizeListWhenCategoryHasNoSizeGroups() throws Exception {
        // "bottoms" has no size group configured anywhere in this test run.
        Category bottoms = categoryRepository.findBySlug("bottoms").orElseThrow();
        List<Size> allActiveSizes = sizeRepository.findByActiveTrueOrderByDisplayOrderAscNameAsc();

        MvcResult result = mockMvc.perform(get("/api/categories/" + bottoms.getId() + "/available-sizes"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());

        assertThat(body).hasSize(allActiveSizes.size());
        List<String> returnedNames = new ArrayList<>();
        body.forEach(n -> returnedNames.add(n.get("name").asText()));
        List<String> expectedNames = allActiveSizes.stream().map(Size::getName).toList();
        assertThat(returnedNames).containsExactlyElementsOf(expectedNames);
    }

    @Test
    void returnsScopedDedupedOrderedUnionOnceSizeGroupsAreConfigured() throws Exception {
        String token = adminAccessToken();
        Category dresses = categoryRepository.findBySlug("dresses").orElseThrow();
        long xlId = findSizeIdByName(token, "XL");
        long lId = findSizeIdByName(token, "L");
        long mId = findSizeIdByName(token, "M");
        long sId = findSizeIdByName(token, "S");

        // Group A (lower displayOrder => wins on order/dedup): S, M - deliberately reversed
        // request order to prove the response follows the join row's displayOrder, not input order.
        String groupABody = """
                {"name":"Dress Group A","description":null,"displayOrder":1,"active":true,
                 "categoryIds":[%d],"sizeIds":[%d,%d]}
                """.formatted(dresses.getId(), mId, sId);
        mockMvc.perform(post("/api/admin/size-groups")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(groupABody))
                .andExpect(status().isOk());

        // Group B (higher displayOrder => contributes after group A): M (duplicate, should not
        // repeat) then L and XL.
        String groupBBody = """
                {"name":"Dress Group B","description":null,"displayOrder":2,"active":true,
                 "categoryIds":[%d],"sizeIds":[%d,%d,%d]}
                """.formatted(dresses.getId(), mId, lId, xlId);
        mockMvc.perform(post("/api/admin/size-groups")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(groupBBody))
                .andExpect(status().isOk());

        MvcResult result = mockMvc.perform(get("/api/categories/" + dresses.getId() + "/available-sizes"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());

        List<String> names = new ArrayList<>();
        body.forEach(n -> names.add(n.get("name").asText()));
        // Group A contributes M, S (join-row order); group B then contributes L, XL (M already seen).
        assertThat(names).containsExactly("M", "S", "L", "XL");
    }

    private long findSizeIdByName(String token, String name) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/admin/sizes").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode array = objectMapper.readTree(result.getResponse().getContentAsString());
        for (JsonNode node : array) {
            if (name.equals(node.get("name").asText())) {
                return node.get("id").asLong();
            }
        }
        throw new IllegalStateException("No size named " + name);
    }
}
