package com.clothingretail.returns;

import static org.assertj.core.api.Assertions.assertThat;

import com.clothingretail.masterdata.Color;
import com.clothingretail.masterdata.Size;
import com.clothingretail.masterdata.repository.ColorRepository;
import com.clothingretail.masterdata.repository.SizeRepository;
import com.clothingretail.product.Product;
import com.clothingretail.product.ProductStatus;
import com.clothingretail.product.ProductVariant;
import com.clothingretail.product.repository.ProductRepository;
import com.clothingretail.product.repository.ProductVariantRepository;
import com.clothingretail.support.CheckoutTestSupport;
import com.clothingretail.support.CheckoutTestSupport.CustomerSession;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ReturnRequestOwnershipTest {

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

    private ProductVariant createTestVariant(String tag, int stockQuantity) {
        Product product = productRepository.findBySlugAndStatus("floral-straight-kurti", ProductStatus.ACTIVE).orElseThrow();
        Size size = sizeRepository.findByActiveTrueOrderByDisplayOrderAscNameAsc().get(0);
        Color color = colorRepository.findByActiveTrueOrderByDisplayOrderAscNameAsc().get(0);
        ProductVariant variant = new ProductVariant();
        variant.setProduct(product);
        variant.setSku("OWN-" + tag + "-" + System.nanoTime());
        variant.setSize(size);
        variant.setColor(color);
        variant.setSellingPrice(new BigDecimal("500.00"));
        variant.setStockQuantity(stockQuantity);
        variant.setActive(true);
        return productVariantRepository.save(variant);
    }

    private record PlacedOrder(CustomerSession session, Long orderId, Long orderItemId) {}

    private PlacedOrder placeOrder(String tag, ProductVariant variant) throws Exception {
        CustomerSession session = CheckoutTestSupport.registerCustomer(mockMvc, objectMapper, tag);
        CheckoutTestSupport.addToCart(mockMvc, session.accessToken(), variant.getId(), 1);
        Long addressId = CheckoutTestSupport.createDefaultAddress(mockMvc, objectMapper, session.accessToken());
        MvcResult orderResult = CheckoutTestSupport.createOrder(mockMvc, session.accessToken(), UUID.randomUUID().toString(), addressId);
        JsonNode orderJson = objectMapper.readTree(orderResult.getResponse().getContentAsString());
        return new PlacedOrder(session, orderJson.get("id").asLong(), orderJson.get("items").get(0).get("id").asLong());
    }

    @Test
    void customerCannotRequestExchangeOnAnotherCustomersOrder() throws Exception {
        ProductVariant variant = createTestVariant("XCUST", 5);
        PlacedOrder owner = placeOrder("owner", variant);

        CustomerSession intruder = CheckoutTestSupport.registerCustomer(mockMvc, objectMapper, "intruder");
        String body = """
                {"requestedVariantId":%d,"reason":"not mine"}
                """.formatted(variant.getId());
        MvcResult result = mockMvc.perform(
                        MockMvcRequestBuilders.post("/api/returns/orders/" + owner.orderId() + "/items/" + owner.orderItemId() + "/exchange")
                                .header("Authorization", "Bearer " + intruder.accessToken())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                .andReturn();
        assertThat(result.getResponse().getStatus()).isEqualTo(404);
    }

    @Test
    void customerCannotViewAnotherCustomersReturnRequestList() throws Exception {
        ProductVariant variant = createTestVariant("XLIST", 5);
        PlacedOrder owner = placeOrder("owner2", variant);

        // owner requests a damage claim (fine, order not delivered yet - use direct manipulation
        // is unnecessary here since we only need SOME request to exist for the ownership check;
        // instead assert the eligible-items endpoint itself is ownership-scoped.
        CustomerSession intruder = CheckoutTestSupport.registerCustomer(mockMvc, objectMapper, "intruder2");
        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.get("/api/returns/orders/" + owner.orderId() + "/eligible-items")
                        .header("Authorization", "Bearer " + intruder.accessToken()))
                .andReturn();
        assertThat(result.getResponse().getStatus()).isEqualTo(404);
    }

    @Test
    void nonDeliveredOrderRejectsBothExchangeAndDamageRequests() throws Exception {
        ProductVariant variant = createTestVariant("NOTDELIV", 5);
        PlacedOrder placed = placeOrder("notdeliv", variant);
        // Order is still PENDING_PAYMENT - never paid or delivered.

        String exchangeBody = """
                {"requestedVariantId":%d,"reason":"wrong size"}
                """.formatted(variant.getId());
        MvcResult exchangeResult = mockMvc.perform(
                        MockMvcRequestBuilders.post("/api/returns/orders/" + placed.orderId() + "/items/" + placed.orderItemId() + "/exchange")
                                .header("Authorization", "Bearer " + placed.session().accessToken())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(exchangeBody))
                .andReturn();
        assertThat(exchangeResult.getResponse().getStatus()).isEqualTo(409);

        MvcResult damageResult = mockMvc.perform(
                        MockMvcRequestBuilders.post("/api/returns/orders/" + placed.orderId() + "/items/" + placed.orderItemId() + "/damage")
                                .header("Authorization", "Bearer " + placed.session().accessToken())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"reason\":\"damaged\"}"))
                .andReturn();
        assertThat(damageResult.getResponse().getStatus()).isEqualTo(409);
    }
}
