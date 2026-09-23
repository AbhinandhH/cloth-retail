package com.clothingretail.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/** Covers the per-order GST invoice PDF endpoint: eligibility (only once payment is confirmed), customer ownership, and the admin equivalent. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OrderInvoiceIntegrationTest {

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

    private ProductVariant createTestVariant(String tag) {
        Product product = productRepository.findBySlugAndStatus("floral-straight-kurti", ProductStatus.ACTIVE).orElseThrow();
        Size size = sizeRepository.findByActiveTrueOrderByDisplayOrderAscNameAsc().get(0);
        Color color = colorRepository.findByActiveTrueOrderByDisplayOrderAscNameAsc().get(0);

        ProductVariant variant = new ProductVariant();
        variant.setProduct(product);
        variant.setSku("INV-" + tag + "-" + System.nanoTime());
        variant.setSize(size);
        variant.setColor(color);
        variant.setSellingPrice(new BigDecimal("500.00"));
        variant.setDiscountPercent(BigDecimal.ZERO);
        variant.setStockQuantity(10);
        variant.setActive(true);
        return productVariantRepository.save(variant);
    }

    private Long placeAndPayOrder(CustomerSession session, ProductVariant variant) throws Exception {
        CheckoutTestSupport.addToCart(mockMvc, session.accessToken(), variant.getId(), 1);
        Long addressId = CheckoutTestSupport.createDefaultAddress(mockMvc, objectMapper, session.accessToken());
        MvcResult orderResult =
                CheckoutTestSupport.createOrder(mockMvc, session.accessToken(), UUID.randomUUID().toString(), addressId);
        Long orderId = objectMapper.readTree(orderResult.getResponse().getContentAsString()).get("id").asLong();

        MvcResult initiateResult = CheckoutTestSupport.initiatePayment(mockMvc, session.accessToken(), orderId);
        String gatewayReference =
                objectMapper.readTree(initiateResult.getResponse().getContentAsString()).get("gatewayReference").asText();
        CheckoutTestSupport.simulatePayment(mockMvc, session.accessToken(), gatewayReference, "SUCCESS");
        return orderId;
    }

    @Test
    void customerCanDownloadInvoiceForAPaidOrder() throws Exception {
        ProductVariant variant = createTestVariant("PAID");
        CustomerSession session = CheckoutTestSupport.registerCustomer(mockMvc, objectMapper, "invoice-paid");
        Long orderId = placeAndPayOrder(session, variant);

        MvcResult result = mockMvc.perform(
                        get("/api/orders/" + orderId + "/invoice").header("Authorization", "Bearer " + session.accessToken()))
                .andReturn();

        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        assertThat(result.getResponse().getContentType()).isEqualTo("application/pdf");
        assertThat(result.getResponse().getContentAsByteArray().length).isGreaterThan(0);
    }

    @Test
    void invoiceUnavailableBeforePaymentIsConfirmed() throws Exception {
        ProductVariant variant = createTestVariant("UNPAID");
        CustomerSession session = CheckoutTestSupport.registerCustomer(mockMvc, objectMapper, "invoice-unpaid");
        CheckoutTestSupport.addToCart(mockMvc, session.accessToken(), variant.getId(), 1);
        Long addressId = CheckoutTestSupport.createDefaultAddress(mockMvc, objectMapper, session.accessToken());
        MvcResult orderResult =
                CheckoutTestSupport.createOrder(mockMvc, session.accessToken(), UUID.randomUUID().toString(), addressId);
        Long orderId = objectMapper.readTree(orderResult.getResponse().getContentAsString()).get("id").asLong();

        MvcResult result = mockMvc.perform(
                        get("/api/orders/" + orderId + "/invoice").header("Authorization", "Bearer " + session.accessToken()))
                .andReturn();

        assertThat(result.getResponse().getStatus()).isEqualTo(409);
        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(json.get("message").asText()).contains("Invoice isn't available");
    }

    @Test
    void customerCannotDownloadAnotherCustomersInvoice() throws Exception {
        ProductVariant variant = createTestVariant("OWNERSHIP");
        CustomerSession owner = CheckoutTestSupport.registerCustomer(mockMvc, objectMapper, "invoice-owner");
        Long orderId = placeAndPayOrder(owner, variant);

        CustomerSession intruder = CheckoutTestSupport.registerCustomer(mockMvc, objectMapper, "invoice-intruder");
        MvcResult result = mockMvc.perform(
                        get("/api/orders/" + orderId + "/invoice").header("Authorization", "Bearer " + intruder.accessToken()))
                .andReturn();

        assertThat(result.getResponse().getStatus()).isEqualTo(404);
    }

    @Test
    void adminCanDownloadInvoiceForAnyPaidOrder() throws Exception {
        ProductVariant variant = createTestVariant("ADMIN");
        CustomerSession session = CheckoutTestSupport.registerCustomer(mockMvc, objectMapper, "invoice-admin");
        Long orderId = placeAndPayOrder(session, variant);

        String adminToken = CheckoutTestSupport.adminAccessToken(mockMvc, objectMapper);
        MvcResult result = mockMvc.perform(
                        get("/api/admin/orders/" + orderId + "/invoice").header("Authorization", "Bearer " + adminToken))
                .andReturn();

        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        assertThat(result.getResponse().getContentType()).isEqualTo("application/pdf");
        assertThat(result.getResponse().getContentAsByteArray().length).isGreaterThan(0);
    }
}
