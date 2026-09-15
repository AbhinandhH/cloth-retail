package com.clothingretail.cart;

import static org.assertj.core.api.Assertions.assertThat;

import com.clothingretail.masterdata.Color;
import com.clothingretail.masterdata.repository.ColorRepository;
import com.clothingretail.masterdata.Size;
import com.clothingretail.masterdata.repository.SizeRepository;
import com.clothingretail.product.Product;
import com.clothingretail.product.ProductRepository;
import com.clothingretail.product.ProductStatus;
import com.clothingretail.product.ProductVariant;
import com.clothingretail.product.ProductVariantRepository;
import com.clothingretail.support.CheckoutTestSupport;
import com.clothingretail.support.CheckoutTestSupport.CustomerSession;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CartIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductVariantRepository productVariantRepository;

    @Autowired
    private SizeRepository sizeRepository;

    @Autowired
    private ColorRepository colorRepository;

    private ProductVariant createTestVariant(String tag, int stockQuantity, boolean active, ProductStatus productStatus) {
        Product product = productRepository.findBySlugAndStatus("floral-straight-kurti", ProductStatus.ACTIVE).orElseThrow();
        if (productStatus != ProductStatus.ACTIVE) {
            // Use a scratch product row so we don't disturb the ACTIVE seeded product other tests rely on.
            Product scratch = new Product();
            scratch.setCategory(product.getCategory());
            scratch.setMaterial(product.getMaterial());
            scratch.setName("Inactive Test Product " + System.nanoTime());
            scratch.setSlug("inactive-test-product-" + System.nanoTime());
            scratch.setStatus(productStatus);
            product = productRepository.save(scratch);
        }
        Size size = sizeRepository.findByActiveTrueOrderByDisplayOrderAscNameAsc().get(0);
        Color color = colorRepository.findByActiveTrueOrderByDisplayOrderAscNameAsc().get(0);

        ProductVariant variant = new ProductVariant();
        variant.setProduct(product);
        variant.setSku("CART-" + tag + "-" + System.nanoTime());
        variant.setSize(size);
        variant.setColor(color);
        variant.setSellingPrice(new BigDecimal("400.00"));
        variant.setDiscountPercent(new BigDecimal("5.00"));
        variant.setStockQuantity(stockQuantity);
        variant.setActive(active);
        return productVariantRepository.save(variant);
    }

    @Test
    void addingItemCreatesCartLazilyAndComputesTotalsLive() throws Exception {
        ProductVariant variant = createTestVariant("ADD", 10, true, ProductStatus.ACTIVE);
        CustomerSession session = CheckoutTestSupport.registerCustomer(mockMvc, objectMapper, "add");

        MvcResult emptyResult = CheckoutTestSupport.getCart(mockMvc, session.accessToken());
        JsonNode emptyJson = objectMapper.readTree(emptyResult.getResponse().getContentAsString());
        assertThat(emptyJson.get("items").size()).isEqualTo(0);

        MvcResult addResult = CheckoutTestSupport.addToCart(mockMvc, session.accessToken(), variant.getId(), 2);
        assertThat(addResult.getResponse().getStatus()).isEqualTo(200);
        JsonNode cartJson = objectMapper.readTree(addResult.getResponse().getContentAsString());
        assertThat(cartJson.get("items").size()).isEqualTo(1);
        assertThat(cartJson.get("itemCount").asInt()).isEqualTo(2);

        JsonNode item = cartJson.get("items").get(0);
        assertThat(item.get("unitPrice").asDouble()).isEqualTo(400.0);
        assertThat(item.get("discountPercent").asDouble()).isEqualTo(5.0);
        // 400 * 2 * 0.95 = 760.00
        assertThat(item.get("lineTotal").asDouble()).isEqualTo(760.0);
        // Cart never reserves stock (only order creation does) - available quantity is still the full 10.
        assertThat(item.get("availableQuantity").asInt()).isEqualTo(10);

        // Adding the same variant again merges quantities rather than duplicating the row.
        MvcResult secondAdd = CheckoutTestSupport.addToCart(mockMvc, session.accessToken(), variant.getId(), 1);
        JsonNode secondJson = objectMapper.readTree(secondAdd.getResponse().getContentAsString());
        assertThat(secondJson.get("items").size()).isEqualTo(1);
        assertThat(secondJson.get("items").get(0).get("quantity").asInt()).isEqualTo(3);
    }

    @Test
    void addingDeactivatedVariantIsRejected() throws Exception {
        ProductVariant inactiveVariant = createTestVariant("INACTIVEVARIANT", 10, false, ProductStatus.ACTIVE);
        CustomerSession session = CheckoutTestSupport.registerCustomer(mockMvc, objectMapper, "inactivevariant");

        MvcResult result = CheckoutTestSupport.addToCart(mockMvc, session.accessToken(), inactiveVariant.getId(), 1);
        assertThat(result.getResponse().getStatus()).isEqualTo(400);
    }

    @Test
    void addingVariantOfInactiveProductIsRejected() throws Exception {
        ProductVariant variant = createTestVariant("INACTIVEPRODUCT", 10, true, ProductStatus.INACTIVE);
        CustomerSession session = CheckoutTestSupport.registerCustomer(mockMvc, objectMapper, "inactiveproduct");

        MvcResult result = CheckoutTestSupport.addToCart(mockMvc, session.accessToken(), variant.getId(), 1);
        assertThat(result.getResponse().getStatus()).isEqualTo(400);
    }

    @Test
    void requestingMoreThanAvailableIsRejected() throws Exception {
        ProductVariant variant = createTestVariant("OVERSTOCK", 2, true, ProductStatus.ACTIVE);
        CustomerSession session = CheckoutTestSupport.registerCustomer(mockMvc, objectMapper, "overstock");

        MvcResult result = CheckoutTestSupport.addToCart(mockMvc, session.accessToken(), variant.getId(), 3);
        assertThat(result.getResponse().getStatus()).isEqualTo(400);
    }

    @Test
    void updateAndRemoveAndClearWorkAndAreOwnershipScoped() throws Exception {
        ProductVariant variant = createTestVariant("LIFECYCLE", 10, true, ProductStatus.ACTIVE);
        CustomerSession owner = CheckoutTestSupport.registerCustomer(mockMvc, objectMapper, "lifecycle-owner");
        MvcResult addResult = CheckoutTestSupport.addToCart(mockMvc, owner.accessToken(), variant.getId(), 2);
        JsonNode addJson = objectMapper.readTree(addResult.getResponse().getContentAsString());
        Long itemId = addJson.get("items").get(0).get("id").asLong();

        // A different customer cannot see or modify the owner's cart item.
        CustomerSession intruder = CheckoutTestSupport.registerCustomer(mockMvc, objectMapper, "lifecycle-intruder");
        MvcResult intruderUpdate = CheckoutTestSupport.updateCartItem(mockMvc, intruder.accessToken(), itemId, 5);
        assertThat(intruderUpdate.getResponse().getStatus()).isEqualTo(404);
        MvcResult intruderRemove = CheckoutTestSupport.removeCartItem(mockMvc, intruder.accessToken(), itemId);
        assertThat(intruderRemove.getResponse().getStatus()).isEqualTo(404);

        // The owner's own update/remove/clear all work.
        MvcResult updateResult = CheckoutTestSupport.updateCartItem(mockMvc, owner.accessToken(), itemId, 4);
        assertThat(updateResult.getResponse().getStatus()).isEqualTo(200);
        JsonNode updateJson = objectMapper.readTree(updateResult.getResponse().getContentAsString());
        assertThat(updateJson.get("items").get(0).get("quantity").asInt()).isEqualTo(4);

        MvcResult clearResult = mockMvc
                .perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/api/cart")
                        .header("Authorization", "Bearer " + owner.accessToken()))
                .andReturn();
        assertThat(clearResult.getResponse().getStatus()).isEqualTo(200);
        JsonNode clearJson = objectMapper.readTree(clearResult.getResponse().getContentAsString());
        assertThat(clearJson.get("items").size()).isEqualTo(0);
    }

    @Test
    void cartEndpointsRequireAuthentication() throws Exception {
        MvcResult result = mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/cart"))
                .andReturn();
        assertThat(result.getResponse().getStatus()).isEqualTo(401);
    }
}
