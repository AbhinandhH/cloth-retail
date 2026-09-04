package com.clothingretail.support;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Shared HTTP-level helpers for the cart/order/payment test suite (registration, address
 * creation, cart/order/payment calls) - pure MockMvc/JSON plumbing, no Spring dependency
 * injection needed, so it's a plain static utility usable from any test class. Individual test
 * classes still autowire their own repositories directly whenever they need to set up precise,
 * isolated product/variant stock levels (see e.g. OrderConcurrencyIntegrationTest).
 */
public final class CheckoutTestSupport {

    private CheckoutTestSupport() {}

    public record CustomerSession(String email, String accessToken) {}

    public static CustomerSession registerCustomer(MockMvc mockMvc, ObjectMapper objectMapper, String tag) throws Exception {
        String email = "customer-" + tag + "-" + System.nanoTime() + "@example.com";
        String body = """
                {"fullName":"Test Customer %s","email":"%s","mobileNumber":"9000000001","password":"Password123!"}
                """.formatted(tag, email);
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andReturn();
        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return new CustomerSession(email, json.get("accessToken").asText());
    }

    public static String adminAccessToken(MockMvc mockMvc, ObjectMapper objectMapper) throws Exception {
        String body = """
                {"email":"admin@clothingretail.local","password":"ChangeMe123!"}
                """;
        MvcResult result = mockMvc.perform(post("/api/admin/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("accessToken").asText();
    }

    public static MvcResult createAddress(MockMvc mockMvc, String accessToken, String content) throws Exception {
        return mockMvc.perform(post("/api/customer/addresses")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(content))
                .andReturn();
    }

    public static Long createDefaultAddress(MockMvc mockMvc, ObjectMapper objectMapper, String accessToken) throws Exception {
        String body = """
                {"label":"Home","addressLine1":"221B Baker Street","city":"Mumbai","state":"Maharashtra","postalCode":"400001","country":"India","isDefault":true}
                """;
        MvcResult result = createAddress(mockMvc, accessToken, body);
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    public static MvcResult addToCart(MockMvc mockMvc, String accessToken, Long productVariantId, int quantity) throws Exception {
        String body = """
                {"productVariantId":%d,"quantity":%d}
                """.formatted(productVariantId, quantity);
        return mockMvc.perform(post("/api/cart/items")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andReturn();
    }

    public static MvcResult updateCartItem(MockMvc mockMvc, String accessToken, Long itemId, int quantity) throws Exception {
        String body = """
                {"quantity":%d}
                """.formatted(quantity);
        return mockMvc.perform(put("/api/cart/items/" + itemId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andReturn();
    }

    public static MvcResult removeCartItem(MockMvc mockMvc, String accessToken, Long itemId) throws Exception {
        return mockMvc.perform(delete("/api/cart/items/" + itemId).header("Authorization", "Bearer " + accessToken))
                .andReturn();
    }

    public static MvcResult getCart(MockMvc mockMvc, String accessToken) throws Exception {
        return mockMvc.perform(get("/api/cart").header("Authorization", "Bearer " + accessToken)).andReturn();
    }

    public static MvcResult createOrder(MockMvc mockMvc, String accessToken, String idempotencyKey, Long addressId) throws Exception {
        String body = """
                {"idempotencyKey":"%s","shippingAddressId":%d}
                """.formatted(idempotencyKey, addressId);
        return mockMvc.perform(post("/api/orders")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andReturn();
    }

    public static MvcResult getOrder(MockMvc mockMvc, String accessToken, Long orderId) throws Exception {
        return mockMvc.perform(get("/api/orders/" + orderId).header("Authorization", "Bearer " + accessToken)).andReturn();
    }

    public static MvcResult initiatePayment(MockMvc mockMvc, String accessToken, Long orderId) throws Exception {
        String body = """
                {"orderId":%d}
                """.formatted(orderId);
        return mockMvc.perform(post("/api/payments/initiate")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andReturn();
    }

    public static MvcResult simulatePayment(MockMvc mockMvc, String accessToken, String gatewayReference, String outcome) throws Exception {
        String body = """
                {"gatewayReference":"%s","outcome":"%s"}
                """.formatted(gatewayReference, outcome);
        return mockMvc.perform(post("/api/payments/mock/simulate")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andReturn();
    }
}
