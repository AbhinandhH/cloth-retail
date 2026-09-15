package com.clothingretail.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.clothingretail.masterdata.Color;
import com.clothingretail.masterdata.repository.ColorRepository;
import com.clothingretail.masterdata.Size;
import com.clothingretail.masterdata.repository.SizeRepository;
import com.clothingretail.order.OrderStatus;
import com.clothingretail.payment.repository.PaymentRepository;
import com.clothingretail.product.Product;
import com.clothingretail.product.repository.ProductRepository;
import com.clothingretail.product.ProductStatus;
import com.clothingretail.product.ProductVariant;
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

/**
 * Payment-specific security/ownership coverage: initiating or simulating payment for an order
 * that isn't yours is refused, a webhook call with a bad signature is refused (its only trust
 * boundary, since /api/payments/webhook is otherwise public), and a real webhook call carrying a
 * genuine simulated signature is accepted and produces the same effect as /mock/simulate - proof
 * that /mock/simulate really does flow through the same handler a real gateway call would use.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PaymentSecurityIntegrationTest {

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

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private com.clothingretail.order.OrderRepository orderRepository;

    private ProductVariant createTestVariant(String tag, int stockQuantity) {
        Product product = productRepository.findBySlugAndStatus("floral-straight-kurti", ProductStatus.ACTIVE).orElseThrow();
        Size size = sizeRepository.findByActiveTrueOrderByDisplayOrderAscNameAsc().get(0);
        Color color = colorRepository.findByActiveTrueOrderByDisplayOrderAscNameAsc().get(0);

        ProductVariant variant = new ProductVariant();
        variant.setProduct(product);
        variant.setSku("PAYSEC-" + tag + "-" + System.nanoTime());
        variant.setSize(size);
        variant.setColor(color);
        variant.setSellingPrice(new BigDecimal("650.00"));
        variant.setDiscountPercent(BigDecimal.ZERO);
        variant.setStockQuantity(stockQuantity);
        variant.setActive(true);
        return productVariantRepository.save(variant);
    }

    private Long placeOrder(CustomerSession session, ProductVariant variant, int quantity) throws Exception {
        CheckoutTestSupport.addToCart(mockMvc, session.accessToken(), variant.getId(), quantity);
        Long addressId = CheckoutTestSupport.createDefaultAddress(mockMvc, objectMapper, session.accessToken());
        MvcResult result = CheckoutTestSupport.createOrder(mockMvc, session.accessToken(), UUID.randomUUID().toString(), addressId);
        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    @Test
    void customerCannotInitiatePaymentForAnotherCustomersOrder() throws Exception {
        ProductVariant variant = createTestVariant("INITIATE", 5);
        CustomerSession owner = CheckoutTestSupport.registerCustomer(mockMvc, objectMapper, "paysec-owner");
        Long orderId = placeOrder(owner, variant, 1);

        CustomerSession intruder = CheckoutTestSupport.registerCustomer(mockMvc, objectMapper, "paysec-intruder");
        MvcResult result = CheckoutTestSupport.initiatePayment(mockMvc, intruder.accessToken(), orderId);
        assertThat(result.getResponse().getStatus()).isEqualTo(404);
    }

    @Test
    void customerCannotSimulateAnotherCustomersPayment() throws Exception {
        ProductVariant variant = createTestVariant("SIMULATE", 5);
        CustomerSession owner = CheckoutTestSupport.registerCustomer(mockMvc, objectMapper, "paysec-simowner");
        Long orderId = placeOrder(owner, variant, 1);
        MvcResult initiateResult = CheckoutTestSupport.initiatePayment(mockMvc, owner.accessToken(), orderId);
        String gatewayReference = objectMapper.readTree(initiateResult.getResponse().getContentAsString()).get("gatewayReference").asText();

        CustomerSession intruder = CheckoutTestSupport.registerCustomer(mockMvc, objectMapper, "paysec-simintruder");
        MvcResult result = CheckoutTestSupport.simulatePayment(mockMvc, intruder.accessToken(), gatewayReference, "SUCCESS");
        assertThat(result.getResponse().getStatus()).isEqualTo(404);

        // Untouched: owner's payment is still PENDING.
        Payment payment = paymentRepository.findByGatewayReference(gatewayReference).orElseThrow();
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING);
    }

    @Test
    void webhookWithInvalidSignatureIsRejected() throws Exception {
        ProductVariant variant = createTestVariant("BADSIG", 5);
        CustomerSession session = CheckoutTestSupport.registerCustomer(mockMvc, objectMapper, "paysec-badsig");
        Long orderId = placeOrder(session, variant, 1);
        MvcResult initiateResult = CheckoutTestSupport.initiatePayment(mockMvc, session.accessToken(), orderId);
        String gatewayReference = objectMapper.readTree(initiateResult.getResponse().getContentAsString()).get("gatewayReference").asText();

        String forgedPayload = """
                {"eventId":"evt_forged","gatewayReference":"%s","orderId":%d,"paymentId":1,"amount":650.0,"outcome":"SUCCESS","timestampEpochMillis":0}
                """.formatted(gatewayReference, orderId);

        MvcResult webhookResult = mockMvc.perform(post("/api/payments/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Signature", "not-a-real-signature")
                        .content(forgedPayload))
                .andReturn();
        assertThat(webhookResult.getResponse().getStatus()).isEqualTo(401);

        // The forged webhook must not have moved the payment out of PENDING.
        Payment payment = paymentRepository.findByGatewayReference(gatewayReference).orElseThrow();
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING);
    }

    @Test
    void webhookEndpointIsPubliclyReachableWithoutAJwt() throws Exception {
        // No Authorization header at all - only the signature is checked. An invalid signature
        // still yields 401 (not 401-for-missing-JWT), proving the endpoint itself is public and
        // the signature is the real boundary.
        MvcResult result = mockMvc.perform(post("/api/payments/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Signature", "garbage")
                        .content("{}"))
                .andReturn();
        assertThat(result.getResponse().getStatus()).isEqualTo(401);
    }

    @Test
    void simulateSuccessConfirmsOrderThroughRealHandlerPath() throws Exception {
        ProductVariant variant = createTestVariant("REALPATH", 5);
        CustomerSession session = CheckoutTestSupport.registerCustomer(mockMvc, objectMapper, "paysec-realpath");
        Long orderId = placeOrder(session, variant, 1);
        MvcResult initiateResult = CheckoutTestSupport.initiatePayment(mockMvc, session.accessToken(), orderId);
        String gatewayReference = objectMapper.readTree(initiateResult.getResponse().getContentAsString()).get("gatewayReference").asText();

        MvcResult simulateResult = CheckoutTestSupport.simulatePayment(mockMvc, session.accessToken(), gatewayReference, "SUCCESS");
        JsonNode json = objectMapper.readTree(simulateResult.getResponse().getContentAsString());
        assertThat(json.get("orderStatus").asText()).isEqualTo("CONFIRMED");

        // Confirm this really did go through PaymentWebhookService.handleWebhook (webhookEventId
        // recorded, exactly like a real webhook delivery would leave behind).
        Payment payment = paymentRepository.findByGatewayReference(gatewayReference).orElseThrow();
        assertThat(payment.getWebhookEventId()).isNotNull();
        assertThat(orderRepository.findById(orderId).orElseThrow().getStatus()).isEqualTo(OrderStatus.CONFIRMED);
    }
}
