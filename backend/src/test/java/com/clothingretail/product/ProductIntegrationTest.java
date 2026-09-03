package com.clothingretail.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.clothingretail.masterdata.Category;
import com.clothingretail.masterdata.CategoryRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Exercises GET /api/products against the Flyway-seeded product catalog
 * (V3__seed_products.sql: 10 products, 3 of them in the "kurtis" category).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProductIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private CategoryRepository categoryRepository;

    @Test
    void listIsPaginated() throws Exception {
        MvcResult firstPage = mockMvc.perform(get("/api/products").param("page", "0").param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page", org.hamcrest.Matchers.is(0)))
                .andExpect(jsonPath("$.size", org.hamcrest.Matchers.is(5)))
                .andReturn();

        JsonNode body = objectMapper.readTree(firstPage.getResponse().getContentAsString());
        assertThat(body.get("content")).hasSizeLessThanOrEqualTo(5);
        assertThat(body.get("totalElements").asInt()).isGreaterThanOrEqualTo(10);
        assertThat(body.get("totalPages").asInt()).isGreaterThanOrEqualTo(2);
    }

    @Test
    void categoryFilterReturnsOnlyThatCategory() throws Exception {
        Optional<Category> kurtis = categoryRepository.findAll().stream()
                .filter(c -> c.getSlug().equals("kurtis"))
                .findFirst();
        assertThat(kurtis).isPresent();

        MvcResult result = mockMvc.perform(get("/api/products")
                        .param("categoryId", String.valueOf(kurtis.get().getId()))
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(body.get("content")).isNotEmpty();
        for (JsonNode product : body.get("content")) {
            assertThat(product.get("categoryName").asText()).isEqualTo("Kurtis");
        }
    }

    @Test
    void categoryAndSearchFiltersCombineWithAnd() throws Exception {
        Optional<Category> kurtis = categoryRepository.findAll().stream()
                .filter(c -> c.getSlug().equals("kurtis"))
                .findFirst();
        assertThat(kurtis).isPresent();

        // "floral" matches two kurti product names in the seed data; combined with the
        // category filter (AND) it should still only return kurtis.
        MvcResult result = mockMvc.perform(get("/api/products")
                        .param("categoryId", String.valueOf(kurtis.get().getId()))
                        .param("q", "floral")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(body.get("content")).isNotEmpty();
        for (JsonNode product : body.get("content")) {
            assertThat(product.get("categoryName").asText()).isEqualTo("Kurtis");
            assertThat(product.get("name").asText().toLowerCase()).contains("floral");
        }
    }

    @Test
    void productDetailReturnsVariantsWithImages() throws Exception {
        mockMvc.perform(get("/api/products/floral-straight-kurti"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slug", org.hamcrest.Matchers.is("floral-straight-kurti")))
                .andExpect(jsonPath("$.variants", org.hamcrest.Matchers.not(org.hamcrest.Matchers.empty())))
                .andExpect(jsonPath("$.variants[0].images", org.hamcrest.Matchers.not(org.hamcrest.Matchers.empty())));
    }

    @Test
    void unknownSlugReturnsStandardNotFoundShape() throws Exception {
        mockMvc.perform(get("/api/products/does-not-exist"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", org.hamcrest.Matchers.is(404)))
                .andExpect(jsonPath("$.path", org.hamcrest.Matchers.is("/api/products/does-not-exist")));
    }
}
