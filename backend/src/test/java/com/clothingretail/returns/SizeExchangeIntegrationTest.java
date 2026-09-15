package com.clothingretail.returns;

import static org.assertj.core.api.Assertions.assertThat;

import com.clothingretail.inventory.InventoryTransaction;
import com.clothingretail.inventory.InventoryTransactionType;
import com.clothingretail.inventory.repository.InventoryTransactionRepository;
import com.clothingretail.masterdata.Color;
import com.clothingretail.masterdata.Size;
import com.clothingretail.masterdata.repository.ColorRepository;
import com.clothingretail.masterdata.repository.SizeRepository;
import com.clothingretail.payment.repository.RefundRepository;
import com.clothingretail.product.Product;
import com.clothingretail.product.ProductStatus;
import com.clothingretail.product.ProductVariant;
import com.clothingretail.product.repository.ProductRepository;
import com.clothingretail.product.repository.ProductVariantRepository;
import com.clothingretail.returns.repository.ReturnRequestRepository;
import com.clothingretail.support.CheckoutTestSupport;
import com.clothingretail.support.CheckoutTestSupport.CustomerSession;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.List;
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
class SizeExchangeIntegrationTest {

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
    private InventoryTransactionRepository inventoryTransactionRepository;

    @Autowired
    private ReturnRequestRepository returnRequestRepository;

    @Autowired
    private RefundRepository refundRepository;

    private ProductVariant createTestVariant(String tag, Size size, Color color, int stockQuantity) {
        Product product = productRepository.findBySlugAndStatus("floral-straight-kurti", ProductStatus.ACTIVE).orElseThrow();
        ProductVariant variant = new ProductVariant();
        variant.setProduct(product);
        variant.setSku("EXCH-" + tag + "-" + System.nanoTime());
        variant.setSize(size);
        variant.setColor(color);
        variant.setSellingPrice(new BigDecimal("500.00"));
        variant.setStockQuantity(stockQuantity);
        variant.setActive(true);
        return productVariantRepository.save(variant);
    }

    /** Places, pays, and delivers an order for {@code variant} - returns (session, orderId, orderItemId). */
    private record DeliveredOrder(CustomerSession session, Long orderId, Long orderItemId) {}

    private DeliveredOrder setUpDeliveredOrder(String tag, ProductVariant variant) throws Exception {
        CustomerSession session = CheckoutTestSupport.registerCustomer(mockMvc, objectMapper, tag);
        CheckoutTestSupport.addToCart(mockMvc, session.accessToken(), variant.getId(), 1);
        Long addressId = CheckoutTestSupport.createDefaultAddress(mockMvc, objectMapper, session.accessToken());

        MvcResult orderResult = CheckoutTestSupport.createOrder(mockMvc, session.accessToken(), UUID.randomUUID().toString(), addressId);
        JsonNode orderJson = objectMapper.readTree(orderResult.getResponse().getContentAsString());
        Long orderId = orderJson.get("id").asLong();
        Long orderItemId = orderJson.get("items").get(0).get("id").asLong();

        MvcResult initiateResult = CheckoutTestSupport.initiatePayment(mockMvc, session.accessToken(), orderId);
        String gatewayReference = objectMapper.readTree(initiateResult.getResponse().getContentAsString()).get("gatewayReference").asText();
        CheckoutTestSupport.simulatePayment(mockMvc, session.accessToken(), gatewayReference, "SUCCESS");

        String adminToken = CheckoutTestSupport.adminAccessToken(mockMvc, objectMapper);
        CheckoutTestSupport.driveOrderToDelivered(mockMvc, adminToken, orderId);

        return new DeliveredOrder(session, orderId, orderItemId);
    }

    private MvcResult requestExchange(CustomerSession session, Long orderId, Long orderItemId, Long requestedVariantId) throws Exception {
        String body = """
                {"requestedVariantId":%d,"reason":"Wrong size, need a different one"}
                """.formatted(requestedVariantId);
        return mockMvc.perform(MockMvcRequestBuilders.post("/api/returns/orders/" + orderId + "/items/" + orderItemId + "/exchange")
                        .header("Authorization", "Bearer " + session.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andReturn();
    }

    private MvcResult approveExchange(String adminToken, Long requestId) throws Exception {
        return mockMvc.perform(MockMvcRequestBuilders.post("/api/admin/returns/" + requestId + "/approve-exchange")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"note\":\"Looks good\"}"))
                .andReturn();
    }

    @Test
    void happyPathRequestThenApproveSwapsStockAtomicallyAndWritesTransactions() throws Exception {
        Size sizeS = sizeRepository.findByActiveTrueOrderByDisplayOrderAscNameAsc().get(0);
        Size sizeM = sizeRepository.findByActiveTrueOrderByDisplayOrderAscNameAsc().get(1);
        Color color = colorRepository.findByActiveTrueOrderByDisplayOrderAscNameAsc().get(0);
        ProductVariant original = createTestVariant("HAPPY-ORIG", sizeS, color, 5);
        ProductVariant replacement = createTestVariant("HAPPY-REPL", sizeM, color, 3);

        DeliveredOrder delivered = setUpDeliveredOrder("exchange-happy", original);
        long refundCountBefore = refundRepository.count();

        MvcResult requestResult = requestExchange(delivered.session(), delivered.orderId(), delivered.orderItemId(), replacement.getId());
        assertThat(requestResult.getResponse().getStatus()).isEqualTo(200);
        Long requestId = objectMapper.readTree(requestResult.getResponse().getContentAsString()).get("id").asLong();

        // Nothing touched yet at request time.
        ProductVariant originalAfterRequest = productVariantRepository.findById(original.getId()).orElseThrow();
        ProductVariant replacementAfterRequest = productVariantRepository.findById(replacement.getId()).orElseThrow();
        assertThat(originalAfterRequest.getStockQuantity()).isEqualTo(4); // 5 - 1 sold at order confirmation
        assertThat(replacementAfterRequest.getStockQuantity()).isEqualTo(3);

        String adminToken = CheckoutTestSupport.adminAccessToken(mockMvc, objectMapper);
        MvcResult approveResult = approveExchange(adminToken, requestId);
        assertThat(approveResult.getResponse().getStatus()).isEqualTo(200);
        JsonNode approveJson = objectMapper.readTree(approveResult.getResponse().getContentAsString());
        assertThat(approveJson.get("status").asText()).isEqualTo("APPROVED");

        ProductVariant originalAfterApprove = productVariantRepository.findById(original.getId()).orElseThrow();
        ProductVariant replacementAfterApprove = productVariantRepository.findById(replacement.getId()).orElseThrow();
        assertThat(originalAfterApprove.getStockQuantity()).isEqualTo(5); // restocked +1
        assertThat(replacementAfterApprove.getStockQuantity()).isEqualTo(2); // decremented -1

        List<InventoryTransaction> originalTransactions = inventoryTransactionRepository.findByProductVariantId(original.getId());
        assertThat(originalTransactions).anySatisfy(t -> {
            assertThat(t.getType()).isEqualTo(InventoryTransactionType.RETURN_IN);
            assertThat(t.getQuantity()).isEqualTo(1);
        });
        List<InventoryTransaction> replacementTransactions = inventoryTransactionRepository.findByProductVariantId(replacement.getId());
        assertThat(replacementTransactions).anySatisfy(t -> {
            assertThat(t.getType()).isEqualTo(InventoryTransactionType.SALE_OUT);
            assertThat(t.getQuantity()).isEqualTo(1);
        });

        // No refund ever created for an exchange.
        assertThat(refundRepository.count()).isEqualTo(refundCountBefore);
    }

    @Test
    void rejectsRequestWhenReplacementSizeHasNoStock() throws Exception {
        Size sizeS = sizeRepository.findByActiveTrueOrderByDisplayOrderAscNameAsc().get(0);
        Size sizeM = sizeRepository.findByActiveTrueOrderByDisplayOrderAscNameAsc().get(1);
        Color color = colorRepository.findByActiveTrueOrderByDisplayOrderAscNameAsc().get(0);
        ProductVariant original = createTestVariant("NOSTOCK-ORIG", sizeS, color, 5);
        ProductVariant replacement = createTestVariant("NOSTOCK-REPL", sizeM, color, 0);

        DeliveredOrder delivered = setUpDeliveredOrder("exchange-nostock", original);

        MvcResult requestResult = requestExchange(delivered.session(), delivered.orderId(), delivered.orderItemId(), replacement.getId());
        assertThat(requestResult.getResponse().getStatus()).isEqualTo(409);
    }

    @Test
    void rejectsApprovalWhenStockMovedBetweenRequestAndApproval() throws Exception {
        Size sizeS = sizeRepository.findByActiveTrueOrderByDisplayOrderAscNameAsc().get(0);
        Size sizeM = sizeRepository.findByActiveTrueOrderByDisplayOrderAscNameAsc().get(1);
        Color color = colorRepository.findByActiveTrueOrderByDisplayOrderAscNameAsc().get(0);
        ProductVariant original = createTestVariant("RACE-ORIG", sizeS, color, 5);
        ProductVariant replacement = createTestVariant("RACE-REPL", sizeM, color, 1);

        DeliveredOrder delivered = setUpDeliveredOrder("exchange-race", original);
        MvcResult requestResult = requestExchange(delivered.session(), delivered.orderId(), delivered.orderItemId(), replacement.getId());
        Long requestId = objectMapper.readTree(requestResult.getResponse().getContentAsString()).get("id").asLong();

        // Stock moves to zero between request and approval (someone else bought the last one).
        replacement.setStockQuantity(0);
        productVariantRepository.save(replacement);

        String adminToken = CheckoutTestSupport.adminAccessToken(mockMvc, objectMapper);
        MvcResult approveResult = approveExchange(adminToken, requestId);
        assertThat(approveResult.getResponse().getStatus()).isEqualTo(409);

        // No partial state: original was never restocked.
        ProductVariant originalAfter = productVariantRepository.findById(original.getId()).orElseThrow();
        assertThat(originalAfter.getStockQuantity()).isEqualTo(4);
    }

    @Test
    void preventsDuplicateActiveRequestsForTheSameItemButAllowsANewOneAfterRejection() throws Exception {
        Size sizeS = sizeRepository.findByActiveTrueOrderByDisplayOrderAscNameAsc().get(0);
        Size sizeM = sizeRepository.findByActiveTrueOrderByDisplayOrderAscNameAsc().get(1);
        Color color = colorRepository.findByActiveTrueOrderByDisplayOrderAscNameAsc().get(0);
        ProductVariant original = createTestVariant("DUP-ORIG", sizeS, color, 5);
        ProductVariant replacement = createTestVariant("DUP-REPL", sizeM, color, 5);

        DeliveredOrder delivered = setUpDeliveredOrder("exchange-dup", original);

        MvcResult first = requestExchange(delivered.session(), delivered.orderId(), delivered.orderItemId(), replacement.getId());
        assertThat(first.getResponse().getStatus()).isEqualTo(200);
        Long firstRequestId = objectMapper.readTree(first.getResponse().getContentAsString()).get("id").asLong();

        MvcResult second = requestExchange(delivered.session(), delivered.orderId(), delivered.orderItemId(), replacement.getId());
        assertThat(second.getResponse().getStatus()).isEqualTo(409);

        String adminToken = CheckoutTestSupport.adminAccessToken(mockMvc, objectMapper);
        mockMvc.perform(MockMvcRequestBuilders.post("/api/admin/returns/" + firstRequestId + "/reject")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"note\":\"Not eligible\"}"));

        MvcResult third = requestExchange(delivered.session(), delivered.orderId(), delivered.orderItemId(), replacement.getId());
        assertThat(third.getResponse().getStatus()).isEqualTo(200);
    }

    @Test
    void rejectsRequestWhenOrderIsNotYetDelivered() throws Exception {
        Size sizeS = sizeRepository.findByActiveTrueOrderByDisplayOrderAscNameAsc().get(0);
        Size sizeM = sizeRepository.findByActiveTrueOrderByDisplayOrderAscNameAsc().get(1);
        Color color = colorRepository.findByActiveTrueOrderByDisplayOrderAscNameAsc().get(0);
        ProductVariant original = createTestVariant("NOTDELIV-ORIG", sizeS, color, 5);
        ProductVariant replacement = createTestVariant("NOTDELIV-REPL", sizeM, color, 5);

        CustomerSession session = CheckoutTestSupport.registerCustomer(mockMvc, objectMapper, "exchange-notdeliv");
        CheckoutTestSupport.addToCart(mockMvc, session.accessToken(), original.getId(), 1);
        Long addressId = CheckoutTestSupport.createDefaultAddress(mockMvc, objectMapper, session.accessToken());
        MvcResult orderResult = CheckoutTestSupport.createOrder(mockMvc, session.accessToken(), UUID.randomUUID().toString(), addressId);
        JsonNode orderJson = objectMapper.readTree(orderResult.getResponse().getContentAsString());
        Long orderId = orderJson.get("id").asLong();
        Long orderItemId = orderJson.get("items").get(0).get("id").asLong();
        // Order stays PENDING_PAYMENT - never paid.

        MvcResult requestResult = requestExchange(session, orderId, orderItemId, replacement.getId());
        assertThat(requestResult.getResponse().getStatus()).isEqualTo(409);
    }
}
