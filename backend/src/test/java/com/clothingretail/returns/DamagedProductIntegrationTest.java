package com.clothingretail.returns;

import static org.assertj.core.api.Assertions.assertThat;

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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DamagedProductIntegrationTest {

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
    private RefundRepository refundRepository;

    private ProductVariant createTestVariant(String tag, int stockQuantity) {
        Product product = productRepository.findBySlugAndStatus("floral-straight-kurti", ProductStatus.ACTIVE).orElseThrow();
        Size size = sizeRepository.findByActiveTrueOrderByDisplayOrderAscNameAsc().get(0);
        Color color = colorRepository.findByActiveTrueOrderByDisplayOrderAscNameAsc().get(0);
        ProductVariant variant = new ProductVariant();
        variant.setProduct(product);
        variant.setSku("DMG-" + tag + "-" + System.nanoTime());
        variant.setSize(size);
        variant.setColor(color);
        variant.setSellingPrice(new BigDecimal("500.00"));
        variant.setStockQuantity(stockQuantity);
        variant.setActive(true);
        return productVariantRepository.save(variant);
    }

    private record DeliveredOrder(CustomerSession session, Long orderId, Long orderItemId, BigDecimal totalAmount) {}

    private DeliveredOrder setUpDeliveredOrder(String tag, ProductVariant variant) throws Exception {
        CustomerSession session = CheckoutTestSupport.registerCustomer(mockMvc, objectMapper, tag);
        CheckoutTestSupport.addToCart(mockMvc, session.accessToken(), variant.getId(), 1);
        Long addressId = CheckoutTestSupport.createDefaultAddress(mockMvc, objectMapper, session.accessToken());

        MvcResult orderResult = CheckoutTestSupport.createOrder(mockMvc, session.accessToken(), UUID.randomUUID().toString(), addressId);
        JsonNode orderJson = objectMapper.readTree(orderResult.getResponse().getContentAsString());
        Long orderId = orderJson.get("id").asLong();
        Long orderItemId = orderJson.get("items").get(0).get("id").asLong();
        BigDecimal totalAmount = new BigDecimal(orderJson.get("totalAmount").asText());

        MvcResult initiateResult = CheckoutTestSupport.initiatePayment(mockMvc, session.accessToken(), orderId);
        String gatewayReference = objectMapper.readTree(initiateResult.getResponse().getContentAsString()).get("gatewayReference").asText();
        CheckoutTestSupport.simulatePayment(mockMvc, session.accessToken(), gatewayReference, "SUCCESS");

        String adminToken = CheckoutTestSupport.adminAccessToken(mockMvc, objectMapper);
        CheckoutTestSupport.driveOrderToDelivered(mockMvc, adminToken, orderId);

        return new DeliveredOrder(session, orderId, orderItemId, totalAmount);
    }

    private MvcResult reportDamage(CustomerSession session, Long orderId, Long orderItemId) throws Exception {
        return mockMvc.perform(MockMvcRequestBuilders.post("/api/returns/orders/" + orderId + "/items/" + orderItemId + "/damage")
                        .header("Authorization", "Bearer " + session.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Arrived with a torn seam\"}"))
                .andReturn();
    }

    private MvcResult uploadEvidence(String adminToken, Long requestId) throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "damage.mp4", "video/mp4", "fake-video-bytes".getBytes());
        return mockMvc.perform(MockMvcRequestBuilders.multipart("/api/admin/returns/" + requestId + "/evidence")
                        .file(file)
                        .header("Authorization", "Bearer " + adminToken))
                .andReturn();
    }

    private MvcResult approveDamage(String adminToken, Long requestId) throws Exception {
        return mockMvc.perform(MockMvcRequestBuilders.post("/api/admin/returns/" + requestId + "/approve-damage")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"note\":\"Confirmed damaged\"}"))
                .andReturn();
    }

    private MvcResult initiateRefund(String adminToken, Long requestId, BigDecimal amount) throws Exception {
        String body = """
                {"amount":%s,"reason":"Damaged item confirmed"}
                """.formatted(amount);
        return mockMvc.perform(MockMvcRequestBuilders.post("/api/admin/returns/" + requestId + "/refund")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andReturn();
    }

    @Test
    void fullHappyPathReportEvidenceApproveThenRefundWithNoInventoryChange() throws Exception {
        ProductVariant variant = createTestVariant("HAPPY", 5);
        DeliveredOrder delivered = setUpDeliveredOrder("damage-happy", variant);

        MvcResult reportResult = reportDamage(delivered.session(), delivered.orderId(), delivered.orderItemId());
        assertThat(reportResult.getResponse().getStatus()).isEqualTo(200);
        JsonNode reportJson = objectMapper.readTree(reportResult.getResponse().getContentAsString());
        Long requestId = reportJson.get("id").asLong();
        assertThat(reportJson.get("evidenceReferenceCode").asText()).isNotBlank();
        assertThat(reportJson.get("evidenceStatus").asText()).isEqualTo("NOT_SUBMITTED");
        assertThat(reportJson.get("status").asText()).isEqualTo("PENDING");

        String adminToken = CheckoutTestSupport.adminAccessToken(mockMvc, objectMapper);

        // No refund without evidence.
        assertThat(initiateRefund(adminToken, requestId, delivered.totalAmount()).getResponse().getStatus()).isEqualTo(409);

        MvcResult uploadResult = uploadEvidence(adminToken, requestId);
        assertThat(uploadResult.getResponse().getStatus()).isEqualTo(200);
        assertThat(objectMapper.readTree(uploadResult.getResponse().getContentAsString()).get("evidenceStatus").asText())
                .isEqualTo("SUBMITTED");

        // No refund without approval, even with evidence submitted.
        assertThat(initiateRefund(adminToken, requestId, delivered.totalAmount()).getResponse().getStatus()).isEqualTo(409);

        MvcResult approveResult = approveDamage(adminToken, requestId);
        assertThat(approveResult.getResponse().getStatus()).isEqualTo(200);
        assertThat(objectMapper.readTree(approveResult.getResponse().getContentAsString()).get("status").asText()).isEqualTo("APPROVED");

        int stockBeforeRefund = productVariantRepository.findById(variant.getId()).orElseThrow().getStockQuantity();
        int damagedBeforeRefund = productVariantRepository.findById(variant.getId()).orElseThrow().getDamagedQuantity();
        long refundCountBefore = refundRepository.count();

        MvcResult refundResult = initiateRefund(adminToken, requestId, delivered.totalAmount());
        assertThat(refundResult.getResponse().getStatus()).isEqualTo(200);
        JsonNode refundJson = objectMapper.readTree(refundResult.getResponse().getContentAsString());
        assertThat(refundJson.get("status").asText()).isEqualTo("REFUNDED");
        assertThat(refundJson.get("refund").get("status").asText()).isEqualTo("COMPLETED");
        // Delta, not an absolute count - other tests in this class create their own refunds against
        // the same shared test database (no per-test transactional rollback, same convention as
        // OrderPaymentIntegrationTest).
        assertThat(refundRepository.count()).isEqualTo(refundCountBefore + 1);

        // Zero inventory side effects, on either counter.
        ProductVariant afterRefund = productVariantRepository.findById(variant.getId()).orElseThrow();
        assertThat(afterRefund.getStockQuantity()).isEqualTo(stockBeforeRefund);
        assertThat(afterRefund.getDamagedQuantity()).isEqualTo(damagedBeforeRefund);
    }

    @Test
    void refundRejectedForASizeExchangeRequestHitDirectly() throws Exception {
        Size sizeS = sizeRepository.findByActiveTrueOrderByDisplayOrderAscNameAsc().get(0);
        Size sizeM = sizeRepository.findByActiveTrueOrderByDisplayOrderAscNameAsc().get(1);
        Color color = colorRepository.findByActiveTrueOrderByDisplayOrderAscNameAsc().get(0);
        Product product = productRepository.findBySlugAndStatus("floral-straight-kurti", ProductStatus.ACTIVE).orElseThrow();

        ProductVariant original = new ProductVariant();
        original.setProduct(product);
        original.setSku("DMG-XVAR-ORIG-" + System.nanoTime());
        original.setSize(sizeS);
        original.setColor(color);
        original.setSellingPrice(new BigDecimal("500.00"));
        original.setStockQuantity(5);
        original.setActive(true);
        original = productVariantRepository.save(original);

        ProductVariant replacement = new ProductVariant();
        replacement.setProduct(product);
        replacement.setSku("DMG-XVAR-REPL-" + System.nanoTime());
        replacement.setSize(sizeM);
        replacement.setColor(color);
        replacement.setSellingPrice(new BigDecimal("500.00"));
        replacement.setStockQuantity(5);
        replacement.setActive(true);
        replacement = productVariantRepository.save(replacement);

        DeliveredOrder delivered = setUpDeliveredOrder("damage-xtype", original);
        String body = """
                {"requestedVariantId":%d,"reason":"Wrong size"}
                """.formatted(replacement.getId());
        MvcResult exchangeResult = mockMvc.perform(
                        MockMvcRequestBuilders.post("/api/returns/orders/" + delivered.orderId() + "/items/" + delivered.orderItemId() + "/exchange")
                                .header("Authorization", "Bearer " + delivered.session().accessToken())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                .andReturn();
        Long exchangeRequestId = objectMapper.readTree(exchangeResult.getResponse().getContentAsString()).get("id").asLong();

        String adminToken = CheckoutTestSupport.adminAccessToken(mockMvc, objectMapper);
        long refundCountBefore = refundRepository.count();
        // Hit the refund endpoint directly against a SIZE_EXCHANGE request id - the backend must
        // reject this itself, regardless of what the frontend would ever offer.
        MvcResult refundResult = initiateRefund(adminToken, exchangeRequestId, delivered.totalAmount());
        assertThat(refundResult.getResponse().getStatus()).isEqualTo(409);
        assertThat(refundRepository.count()).isEqualTo(refundCountBefore);
    }

    @Test
    void refundNeverProceedsAfterADamageClaimIsRejected() throws Exception {
        ProductVariant variant = createTestVariant("REJECTED", 5);
        DeliveredOrder delivered = setUpDeliveredOrder("damage-rejected", variant);
        MvcResult reportResult = reportDamage(delivered.session(), delivered.orderId(), delivered.orderItemId());
        Long requestId = objectMapper.readTree(reportResult.getResponse().getContentAsString()).get("id").asLong();

        String adminToken = CheckoutTestSupport.adminAccessToken(mockMvc, objectMapper);
        uploadEvidence(adminToken, requestId);

        MvcResult rejectResult = mockMvc.perform(MockMvcRequestBuilders.post("/api/admin/returns/" + requestId + "/reject")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"note\":\"Not actually damaged\"}"))
                .andReturn();
        assertThat(rejectResult.getResponse().getStatus()).isEqualTo(200);

        long refundCountBefore = refundRepository.count();
        MvcResult refundResult = initiateRefund(adminToken, requestId, delivered.totalAmount());
        assertThat(refundResult.getResponse().getStatus()).isEqualTo(409);
        assertThat(refundRepository.count()).isEqualTo(refundCountBefore);
    }

    @Test
    void secondRefundAttemptAfterSuccessIsRejectedAndOnlyOneRefundRowExists() throws Exception {
        ProductVariant variant = createTestVariant("DOUBLE", 5);
        DeliveredOrder delivered = setUpDeliveredOrder("damage-double", variant);
        MvcResult reportResult = reportDamage(delivered.session(), delivered.orderId(), delivered.orderItemId());
        Long requestId = objectMapper.readTree(reportResult.getResponse().getContentAsString()).get("id").asLong();

        String adminToken = CheckoutTestSupport.adminAccessToken(mockMvc, objectMapper);
        uploadEvidence(adminToken, requestId);
        approveDamage(adminToken, requestId);

        long refundCountBefore = refundRepository.count();
        MvcResult firstRefund = initiateRefund(adminToken, requestId, delivered.totalAmount());
        assertThat(firstRefund.getResponse().getStatus()).isEqualTo(200);

        MvcResult secondRefund = initiateRefund(adminToken, requestId, delivered.totalAmount());
        assertThat(secondRefund.getResponse().getStatus()).isEqualTo(409);

        assertThat(refundRepository.count()).isEqualTo(refundCountBefore + 1);
    }

    @Test
    void evidenceStreamingIsOwnershipCheckedAndNeverPubliclyExposed() throws Exception {
        ProductVariant variant = createTestVariant("OWNER", 5);
        DeliveredOrder delivered = setUpDeliveredOrder("damage-owner", variant);
        MvcResult reportResult = reportDamage(delivered.session(), delivered.orderId(), delivered.orderItemId());
        Long requestId = objectMapper.readTree(reportResult.getResponse().getContentAsString()).get("id").asLong();

        String adminToken = CheckoutTestSupport.adminAccessToken(mockMvc, objectMapper);
        uploadEvidence(adminToken, requestId);

        // The owning customer and admin can both stream it.
        MvcResult ownerResult = mockMvc.perform(MockMvcRequestBuilders.get("/api/returns/" + requestId + "/evidence")
                        .header("Authorization", "Bearer " + delivered.session().accessToken()))
                .andReturn();
        assertThat(ownerResult.getResponse().getStatus()).isEqualTo(200);

        MvcResult adminResult = mockMvc.perform(MockMvcRequestBuilders.get("/api/admin/returns/" + requestId + "/evidence")
                        .header("Authorization", "Bearer " + adminToken))
                .andReturn();
        assertThat(adminResult.getResponse().getStatus()).isEqualTo(200);

        // A different customer cannot.
        CustomerSession intruder = CheckoutTestSupport.registerCustomer(mockMvc, objectMapper, "damage-intruder");
        MvcResult intruderResult = mockMvc.perform(MockMvcRequestBuilders.get("/api/returns/" + requestId + "/evidence")
                        .header("Authorization", "Bearer " + intruder.accessToken()))
                .andReturn();
        assertThat(intruderResult.getResponse().getStatus()).isEqualTo(404);

        // An unauthenticated request cannot either.
        MvcResult unauthResult = mockMvc.perform(MockMvcRequestBuilders.get("/api/returns/" + requestId + "/evidence")).andReturn();
        assertThat(unauthResult.getResponse().getStatus()).isEqualTo(401);
    }
}
