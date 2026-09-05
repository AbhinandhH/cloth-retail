package com.clothingretail.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import com.clothingretail.inventory.InventoryTransaction;
import com.clothingretail.inventory.InventoryTransactionRepository;
import com.clothingretail.inventory.InventoryTransactionType;
import com.clothingretail.masterdata.Color;
import com.clothingretail.masterdata.ColorRepository;
import com.clothingretail.masterdata.Size;
import com.clothingretail.masterdata.SizeRepository;
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
import java.time.Instant;
import java.time.temporal.ChronoUnit;
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
 * Coverage for the admin order management feature: the admin-driven status transition graph
 * (valid moves + history, invalid moves rejected, payment-only statuses structurally unreachable
 * as a target), cancellation's stock-reversal-vs-reservation-release branching, refund gating on
 * payment status, role enforcement, and that every system-driven status change (order creation
 * through payment success/failure) leaves a null {@code changedByName} in the history.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminOrderManagementIntegrationTest {

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
    private OrderStatusHistoryRepository orderStatusHistoryRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderReservationCleanupJob cleanupJob;

    // ── fixtures ─────────────────────────────────────────────────────────────

    private ProductVariant createTestVariant(String tag, int stockQuantity) {
        Product product = productRepository.findBySlugAndStatus("floral-straight-kurti", ProductStatus.ACTIVE).orElseThrow();
        Size size = sizeRepository.findByActiveTrueOrderByDisplayOrderAscNameAsc().get(0);
        Color color = colorRepository.findByActiveTrueOrderByDisplayOrderAscNameAsc().get(0);

        ProductVariant variant = new ProductVariant();
        variant.setProduct(product);
        variant.setSku("ADMORD-" + tag + "-" + System.nanoTime());
        variant.setSize(size);
        variant.setColor(color);
        variant.setSellingPrice(new BigDecimal("400.00"));
        variant.setDiscountPercent(BigDecimal.ZERO);
        variant.setStockQuantity(stockQuantity);
        variant.setActive(true);
        return productVariantRepository.save(variant);
    }

    /** Places an order and drives it through to PENDING_PAYMENT (no payment initiated yet). */
    private Long placePendingOrder(String tag, ProductVariant variant, int quantity) throws Exception {
        CustomerSession session = CheckoutTestSupport.registerCustomer(mockMvc, objectMapper, tag);
        CheckoutTestSupport.addToCart(mockMvc, session.accessToken(), variant.getId(), quantity);
        Long addressId = CheckoutTestSupport.createDefaultAddress(mockMvc, objectMapper, session.accessToken());
        MvcResult result = CheckoutTestSupport.createOrder(mockMvc, session.accessToken(), UUID.randomUUID().toString(), addressId);
        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    /** Places an order and drives it all the way to CONFIRMED via a successful simulated payment. */
    private Long placeConfirmedOrder(String tag, ProductVariant variant, int quantity) throws Exception {
        CustomerSession session = CheckoutTestSupport.registerCustomer(mockMvc, objectMapper, tag);
        CheckoutTestSupport.addToCart(mockMvc, session.accessToken(), variant.getId(), quantity);
        Long addressId = CheckoutTestSupport.createDefaultAddress(mockMvc, objectMapper, session.accessToken());
        MvcResult orderResult = CheckoutTestSupport.createOrder(mockMvc, session.accessToken(), UUID.randomUUID().toString(), addressId);
        Long orderId = objectMapper.readTree(orderResult.getResponse().getContentAsString()).get("id").asLong();

        MvcResult initiateResult = CheckoutTestSupport.initiatePayment(mockMvc, session.accessToken(), orderId);
        String gatewayReference = objectMapper.readTree(initiateResult.getResponse().getContentAsString()).get("gatewayReference").asText();
        MvcResult simulateResult = CheckoutTestSupport.simulatePayment(mockMvc, session.accessToken(), gatewayReference, "SUCCESS");
        assertThat(objectMapper.readTree(simulateResult.getResponse().getContentAsString()).get("orderStatus").asText()).isEqualTo("CONFIRMED");
        return orderId;
    }

    /** Places an order and drives its payment to FAILURE, leaving the order PAYMENT_FAILED with a non-SUCCESS payment on record. */
    private Long placePaymentFailedOrder(String tag, ProductVariant variant, int quantity) throws Exception {
        CustomerSession session = CheckoutTestSupport.registerCustomer(mockMvc, objectMapper, tag);
        CheckoutTestSupport.addToCart(mockMvc, session.accessToken(), variant.getId(), quantity);
        Long addressId = CheckoutTestSupport.createDefaultAddress(mockMvc, objectMapper, session.accessToken());
        MvcResult orderResult = CheckoutTestSupport.createOrder(mockMvc, session.accessToken(), UUID.randomUUID().toString(), addressId);
        Long orderId = objectMapper.readTree(orderResult.getResponse().getContentAsString()).get("id").asLong();

        MvcResult initiateResult = CheckoutTestSupport.initiatePayment(mockMvc, session.accessToken(), orderId);
        String gatewayReference = objectMapper.readTree(initiateResult.getResponse().getContentAsString()).get("gatewayReference").asText();
        CheckoutTestSupport.simulatePayment(mockMvc, session.accessToken(), gatewayReference, "FAILURE");
        return orderId;
    }

    private String adminToken() throws Exception {
        return CheckoutTestSupport.adminAccessToken(mockMvc, objectMapper);
    }

    private MvcResult adminStatusUpdate(String adminToken, Long orderId, String toStatus, String reason) throws Exception {
        String body = reason == null
                ? """
                {"toStatus":"%s"}
                """.formatted(toStatus)
                : """
                {"toStatus":"%s","reason":"%s"}
                """.formatted(toStatus, reason);
        return mockMvc.perform(post("/api/admin/orders/" + orderId + "/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andReturn();
    }

    private MvcResult adminCancel(String adminToken, Long orderId, String reason) throws Exception {
        String body = """
                {"reason":"%s"}
                """.formatted(reason);
        return mockMvc.perform(post("/api/admin/orders/" + orderId + "/cancel")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andReturn();
    }

    private MvcResult adminDetail(String adminToken, Long orderId) throws Exception {
        return mockMvc.perform(get("/api/admin/orders/" + orderId).header("Authorization", "Bearer " + adminToken)).andReturn();
    }

    private MvcResult adminRefund(String adminToken, Long orderId, BigDecimal amount) throws Exception {
        String body = """
                {"amount":%s}
                """.formatted(amount);
        return mockMvc.perform(post("/api/admin/orders/" + orderId + "/refund")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andReturn();
    }

    // ── forward transition + history ────────────────────────────────────────

    @Test
    void validTransitionAppliesAndWritesHistoryWithAdminName() throws Exception {
        ProductVariant variant = createTestVariant("VALID", 10);
        Long orderId = placeConfirmedOrder("valid", variant, 2);
        String admin = adminToken();

        MvcResult result = adminStatusUpdate(admin, orderId, "PROCESSING", "packing started");
        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(json.get("status").asText()).isEqualTo("PROCESSING");

        boolean hasPackedNext = false;
        boolean hasCancelledNext = false;
        for (JsonNode next : json.get("availableNextStatuses")) {
            hasPackedNext |= next.asText().equals("PACKED");
            hasCancelledNext |= next.asText().equals("CANCELLED");
        }
        assertThat(hasPackedNext).isTrue();
        assertThat(hasCancelledNext).isTrue();

        JsonNode lastHistoryRow = null;
        for (JsonNode row : json.get("statusHistory")) {
            lastHistoryRow = row;
        }
        assertThat(lastHistoryRow).isNotNull();
        assertThat(lastHistoryRow.get("previousStatus").asText()).isEqualTo("CONFIRMED");
        assertThat(lastHistoryRow.get("newStatus").asText()).isEqualTo("PROCESSING");
        assertThat(lastHistoryRow.get("changedByName").asText()).isEqualTo("Super Admin");
        assertThat(lastHistoryRow.get("reason").asText()).isEqualTo("packing started");

        Order order = orderRepository.findById(orderId).orElseThrow();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PROCESSING);
    }

    @Test
    void invalidTransitionIsRejectedWithClearMessage() throws Exception {
        ProductVariant variant = createTestVariant("INVALID", 10);
        Long orderId = placeConfirmedOrder("invalid", variant, 1);
        String admin = adminToken();

        // CONFIRMED -> SHIPPED directly is not a valid move (must go through PROCESSING, PACKED first).
        MvcResult result = adminStatusUpdate(admin, orderId, "SHIPPED", null);
        assertThat(result.getResponse().getStatus()).isEqualTo(409);
        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        String message = json.get("message").asText();
        assertThat(message).contains("CONFIRMED").contains("SHIPPED").contains("PROCESSING");

        Order order = orderRepository.findById(orderId).orElseThrow();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
    }

    @Test
    void statusEndpointStructurallyCannotAcceptAPaymentOnlyStatus() throws Exception {
        ProductVariant variant = createTestVariant("STRUCT", 10);
        Long orderId = placeConfirmedOrder("struct", variant, 1);
        String admin = adminToken();

        for (String rejected : new String[] {"PENDING_PAYMENT", "PAYMENT_PROCESSING", "PAYMENT_FAILED"}) {
            MvcResult result = adminStatusUpdate(admin, orderId, rejected, null);
            assertThat(result.getResponse().getStatus())
                    .as("toStatus=%s must be rejected at deserialization, not accepted", rejected)
                    .isEqualTo(400);
        }

        Order order = orderRepository.findById(orderId).orElseThrow();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
    }

    // ── cancellation branching ───────────────────────────────────────────────

    @Test
    void cancellingConfirmedPaidOrderRestoresStockViaCancelReversal() throws Exception {
        ProductVariant variant = createTestVariant("CANCELPAID", 20);
        Long orderId = placeConfirmedOrder("cancelpaid", variant, 5);
        String admin = adminToken();

        ProductVariant afterPayment = productVariantRepository.findById(variant.getId()).orElseThrow();
        assertThat(afterPayment.getStockQuantity()).isEqualTo(15);

        MvcResult result = adminCancel(admin, orderId, "customer requested cancellation");
        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(json.get("status").asText()).isEqualTo("CANCELLED");

        ProductVariant afterCancel = productVariantRepository.findById(variant.getId()).orElseThrow();
        assertThat(afterCancel.getStockQuantity()).isEqualTo(20);
        assertThat(afterCancel.getReservedQuantity()).isEqualTo(0);

        InventoryTransaction reversal = inventoryTransactionRepository.findByProductVariantId(variant.getId()).stream()
                .filter(t -> t.getType() == InventoryTransactionType.CANCEL_REVERSAL)
                .findFirst()
                .orElseThrow(() -> new AssertionError("No CANCEL_REVERSAL transaction found"));
        assertThat(reversal.getQuantity()).isEqualTo(5);
        assertThat(reversal.getPreviousQuantity()).isEqualTo(15);
        assertThat(reversal.getNewQuantity()).isEqualTo(20);
        assertThat(reversal.getReferenceType()).isEqualTo("ORDER");
        assertThat(reversal.getReferenceId()).isEqualTo(orderId);
    }

    @Test
    void cancellingPendingPaymentOrderReleasesReservationWithoutInventoryTransaction() throws Exception {
        ProductVariant variant = createTestVariant("CANCELPENDING", 10);
        Long orderId = placePendingOrder("cancelpending", variant, 3);
        String admin = adminToken();

        ProductVariant afterReserve = productVariantRepository.findById(variant.getId()).orElseThrow();
        assertThat(afterReserve.getReservedQuantity()).isEqualTo(3);

        MvcResult result = adminCancel(admin, orderId, "never paid");
        assertThat(result.getResponse().getStatus()).isEqualTo(200);

        ProductVariant afterCancel = productVariantRepository.findById(variant.getId()).orElseThrow();
        assertThat(afterCancel.getReservedQuantity()).isEqualTo(0);
        assertThat(afterCancel.getStockQuantity()).isEqualTo(10);

        boolean anyCancelReversal = inventoryTransactionRepository.findByProductVariantId(variant.getId()).stream()
                .anyMatch(t -> t.getType() == InventoryTransactionType.CANCEL_REVERSAL);
        assertThat(anyCancelReversal).isFalse();
    }

    @Test
    void cancellingShippedOrderIsRejected() throws Exception {
        ProductVariant variant = createTestVariant("CANCELSHIPPED", 10);
        Long orderId = placeConfirmedOrder("cancelshipped", variant, 1);
        String admin = adminToken();

        assertThat(adminStatusUpdate(admin, orderId, "PROCESSING", null).getResponse().getStatus()).isEqualTo(200);
        assertThat(adminStatusUpdate(admin, orderId, "PACKED", null).getResponse().getStatus()).isEqualTo(200);
        assertThat(adminStatusUpdate(admin, orderId, "SHIPPED", null).getResponse().getStatus()).isEqualTo(200);

        MvcResult result = adminCancel(admin, orderId, "too late");
        assertThat(result.getResponse().getStatus()).isEqualTo(409);

        Order order = orderRepository.findById(orderId).orElseThrow();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.SHIPPED);
    }

    // ── role enforcement ─────────────────────────────────────────────────────

    @Test
    void customerTokenIsForbiddenOnEveryAdminOrderRoute() throws Exception {
        ProductVariant variant = createTestVariant("FORBIDDEN", 10);
        Long orderId = placeConfirmedOrder("forbidden", variant, 1);
        CustomerSession intruder = CheckoutTestSupport.registerCustomer(mockMvc, objectMapper, "forbidden-intruder");
        String customerToken = intruder.accessToken();

        assertThat(mockMvc.perform(get("/api/admin/orders").header("Authorization", "Bearer " + customerToken))
                        .andReturn().getResponse().getStatus())
                .isEqualTo(403);
        assertThat(mockMvc.perform(get("/api/admin/orders/dashboard").header("Authorization", "Bearer " + customerToken))
                        .andReturn().getResponse().getStatus())
                .isEqualTo(403);
        assertThat(adminDetail(customerToken, orderId).getResponse().getStatus()).isEqualTo(403);
        assertThat(adminStatusUpdate(customerToken, orderId, "PROCESSING", null).getResponse().getStatus()).isEqualTo(403);
        assertThat(adminCancel(customerToken, orderId, "nope").getResponse().getStatus()).isEqualTo(403);
        assertThat(mockMvc.perform(post("/api/admin/orders/" + orderId + "/notes")
                                .header("Authorization", "Bearer " + customerToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {"note":"hi"}
                                        """))
                        .andReturn().getResponse().getStatus())
                .isEqualTo(403);
        assertThat(mockMvc.perform(put("/api/admin/orders/" + orderId + "/shipment")
                                .header("Authorization", "Bearer " + customerToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {"provider":"DHL"}
                                        """))
                        .andReturn().getResponse().getStatus())
                .isEqualTo(403);
        assertThat(adminRefund(customerToken, orderId, new BigDecimal("10.00")).getResponse().getStatus()).isEqualTo(403);
    }

    // ── refund gating ────────────────────────────────────────────────────────

    @Test
    void refundIsRejectedOnAnUnpaidOrderAndSucceedsOnAPaidOne() throws Exception {
        String admin = adminToken();

        ProductVariant unpaidVariant = createTestVariant("REFUNDUNPAID", 10);
        Long unpaidOrderId = placePaymentFailedOrder("refundunpaid", unpaidVariant, 1);
        MvcResult rejected = adminRefund(admin, unpaidOrderId, new BigDecimal("400.00"));
        assertThat(rejected.getResponse().getStatus()).isEqualTo(409);

        ProductVariant paidVariant = createTestVariant("REFUNDPAID", 10);
        Long paidOrderId = placeConfirmedOrder("refundpaid", paidVariant, 1);
        Order paidOrder = orderRepository.findById(paidOrderId).orElseThrow();
        MvcResult accepted = adminRefund(admin, paidOrderId, paidOrder.getTotalAmount());
        assertThat(accepted.getResponse().getStatus()).isEqualTo(200);
        JsonNode json = objectMapper.readTree(accepted.getResponse().getContentAsString());
        assertThat(json.get("status").asText()).isEqualTo("COMPLETED");
        assertThat(json.get("reference").asText()).startsWith("mock_refund_");
    }

    // ── system-driven history ────────────────────────────────────────────────

    @Test
    void systemDrivenHistoryRowsFromCreationThroughPaymentSuccessHaveNullChangedByName() throws Exception {
        ProductVariant variant = createTestVariant("SYSTEMHISTORY", 10);
        Long orderId = placeConfirmedOrder("systemhistory", variant, 1);
        String admin = adminToken();

        MvcResult result = adminDetail(admin, orderId);
        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        JsonNode history = objectMapper.readTree(result.getResponse().getContentAsString()).get("statusHistory");

        assertThat(history).hasSize(3);
        assertThat(history.get(0).get("previousStatus").isNull()).isTrue();
        assertThat(history.get(0).get("newStatus").asText()).isEqualTo("PENDING_PAYMENT");
        assertThat(history.get(1).get("previousStatus").asText()).isEqualTo("PENDING_PAYMENT");
        assertThat(history.get(1).get("newStatus").asText()).isEqualTo("PAYMENT_PROCESSING");
        assertThat(history.get(2).get("previousStatus").asText()).isEqualTo("PAYMENT_PROCESSING");
        assertThat(history.get(2).get("newStatus").asText()).isEqualTo("CONFIRMED");
        for (JsonNode row : history) {
            assertThat(row.get("changedByName").isNull())
                    .as("system-driven row must have a null changedByName: %s", row)
                    .isTrue();
        }
    }

    @Test
    void systemDrivenHistoryRowFromPaymentFailureHasNullChangedByName() throws Exception {
        ProductVariant variant = createTestVariant("SYSTEMFAIL", 10);
        Long orderId = placePaymentFailedOrder("systemfail", variant, 1);

        var rows = orderStatusHistoryRepository.findByOrderIdOrderByIdAsc(orderId);
        assertThat(rows).hasSize(3);
        OrderStatusHistory lastRow = rows.get(rows.size() - 1);
        assertThat(lastRow.getPreviousStatus()).isEqualTo(OrderStatus.PAYMENT_PROCESSING);
        assertThat(lastRow.getNewStatus()).isEqualTo(OrderStatus.PAYMENT_FAILED);
        assertThat(lastRow.getChangedBy()).isNull();
    }

    @Test
    void systemDrivenHistoryRowFromReservationExpiryHasNullChangedByName() throws Exception {
        ProductVariant variant = createTestVariant("SYSTEMEXPIRE", 10);
        Long orderId = placePendingOrder("systemexpire", variant, 2);

        Order order = orderRepository.findById(orderId).orElseThrow();
        order.setReservationExpiresAt(Instant.now().minus(1, ChronoUnit.HOURS));
        orderRepository.save(order);

        cleanupJob.releaseExpiredReservations();

        var rows = orderStatusHistoryRepository.findByOrderIdOrderByIdAsc(orderId);
        OrderStatusHistory lastRow = rows.get(rows.size() - 1);
        assertThat(lastRow.getPreviousStatus()).isEqualTo(OrderStatus.PENDING_PAYMENT);
        assertThat(lastRow.getNewStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(lastRow.getChangedBy()).isNull();
    }

    // ── list + dashboard (avoiding N+1 via Specification fetch-join + batched aux queries) ────

    @Test
    void listAndDashboardReturnCorrectRowsForAdmin() throws Exception {
        String admin = adminToken();

        ProductVariant confirmedVariant = createTestVariant("LISTCONFIRMED", 10);
        Long confirmedOrderId = placeConfirmedOrder("listconfirmed", confirmedVariant, 2);
        Order confirmedOrder = orderRepository.findById(confirmedOrderId).orElseThrow();

        ProductVariant pendingVariant = createTestVariant("LISTPENDING", 10);
        Long pendingOrderId = placePendingOrder("listpending", pendingVariant, 1);

        MvcResult listResult = mockMvc.perform(get("/api/admin/orders?page=0&size=50")
                        .header("Authorization", "Bearer " + admin))
                .andReturn();
        assertThat(listResult.getResponse().getStatus()).isEqualTo(200);
        JsonNode listJson = objectMapper.readTree(listResult.getResponse().getContentAsString());
        assertThat(listJson.get("totalElements").asLong()).isGreaterThanOrEqualTo(2);

        JsonNode confirmedRow = findRowById(listJson.get("content"), confirmedOrderId);
        assertThat(confirmedRow).isNotNull();
        assertThat(confirmedRow.get("customerName").asText()).isEqualTo("Test Customer listconfirmed");
        assertThat(confirmedRow.get("status").asText()).isEqualTo("CONFIRMED");
        assertThat(confirmedRow.get("paymentStatus").asText()).isEqualTo("SUCCESS");
        assertThat(confirmedRow.get("itemCount").asInt()).isEqualTo(2);
        assertThat(confirmedRow.get("totalAmount").decimalValue()).isEqualByComparingTo(confirmedOrder.getTotalAmount());

        JsonNode pendingRow = findRowById(listJson.get("content"), pendingOrderId);
        assertThat(pendingRow).isNotNull();
        assertThat(pendingRow.get("status").asText()).isEqualTo("PENDING_PAYMENT");
        assertThat(pendingRow.get("paymentStatus").isNull()).isTrue();
        assertThat(pendingRow.get("itemCount").asInt()).isEqualTo(1);

        // orderStatus filter narrows correctly.
        MvcResult filteredResult = mockMvc.perform(get("/api/admin/orders?orderStatus=CONFIRMED&page=0&size=50")
                        .header("Authorization", "Bearer " + admin))
                .andReturn();
        JsonNode filteredJson = objectMapper.readTree(filteredResult.getResponse().getContentAsString());
        assertThat(findRowById(filteredJson.get("content"), confirmedOrderId)).isNotNull();
        assertThat(findRowById(filteredJson.get("content"), pendingOrderId)).isNull();

        // q filter matches by order number.
        MvcResult qResult = mockMvc.perform(get("/api/admin/orders?q=" + confirmedOrder.getOrderNumber() + "&page=0&size=50")
                        .header("Authorization", "Bearer " + admin))
                .andReturn();
        JsonNode qJson = objectMapper.readTree(qResult.getResponse().getContentAsString());
        assertThat(qJson.get("totalElements").asLong()).isEqualTo(1);
        assertThat(qJson.get("content").get(0).get("id").asLong()).isEqualTo(confirmedOrderId);

        // paymentStatus filter (via the latest-payment subquery) narrows correctly.
        MvcResult paymentStatusResult = mockMvc.perform(get("/api/admin/orders?paymentStatus=SUCCESS&page=0&size=50")
                        .header("Authorization", "Bearer " + admin))
                .andReturn();
        JsonNode paymentStatusJson = objectMapper.readTree(paymentStatusResult.getResponse().getContentAsString());
        assertThat(findRowById(paymentStatusJson.get("content"), confirmedOrderId)).isNotNull();
        assertThat(findRowById(paymentStatusJson.get("content"), pendingOrderId)).isNull();

        MvcResult dashboardResult = mockMvc.perform(get("/api/admin/orders/dashboard").header("Authorization", "Bearer " + admin))
                .andReturn();
        assertThat(dashboardResult.getResponse().getStatus()).isEqualTo(200);
        JsonNode dashboardJson = objectMapper.readTree(dashboardResult.getResponse().getContentAsString());
        assertThat(dashboardJson.get("pendingCount").asLong()).isGreaterThanOrEqualTo(1);
        assertThat(dashboardJson.get("recentOrders").size()).isLessThanOrEqualTo(10);
    }

    private JsonNode findRowById(JsonNode rows, Long orderId) {
        for (JsonNode row : rows) {
            if (row.get("id").asLong() == orderId) {
                return row;
            }
        }
        return null;
    }

    // ── notes + shipment smoke coverage ──────────────────────────────────────

    @Test
    void addNoteAndUpsertShipmentSucceedForAdmin() throws Exception {
        ProductVariant variant = createTestVariant("NOTESHIP", 10);
        Long orderId = placeConfirmedOrder("noteship", variant, 1);
        String admin = adminToken();

        MvcResult noteResult = mockMvc.perform(post("/api/admin/orders/" + orderId + "/notes")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"note":"Called customer to confirm size"}
                                """))
                .andReturn();
        assertThat(noteResult.getResponse().getStatus()).isEqualTo(200);
        JsonNode noteJson = objectMapper.readTree(noteResult.getResponse().getContentAsString());
        assertThat(noteJson.get("note").asText()).isEqualTo("Called customer to confirm size");
        assertThat(noteJson.get("adminName").asText()).isEqualTo("Super Admin");

        MvcResult shipmentResult = mockMvc.perform(put("/api/admin/orders/" + orderId + "/shipment")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"provider":"BlueDart","trackingNumber":"BD12345","shipmentDate":"2026-09-10","deliveryDate":null,"notes":"handle with care"}
                                """))
                .andReturn();
        assertThat(shipmentResult.getResponse().getStatus()).isEqualTo(200);
        JsonNode shipmentJson = objectMapper.readTree(shipmentResult.getResponse().getContentAsString());
        assertThat(shipmentJson.get("provider").asText()).isEqualTo("BlueDart");
        assertThat(shipmentJson.get("trackingNumber").asText()).isEqualTo("BD12345");

        // Upsert again (update path) - same order, different tracking number.
        MvcResult secondShipmentResult = mockMvc.perform(put("/api/admin/orders/" + orderId + "/shipment")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"provider":"BlueDart","trackingNumber":"BD99999","shipmentDate":"2026-09-11","deliveryDate":null,"notes":null}
                                """))
                .andReturn();
        assertThat(secondShipmentResult.getResponse().getStatus()).isEqualTo(200);
        JsonNode secondShipmentJson = objectMapper.readTree(secondShipmentResult.getResponse().getContentAsString());
        assertThat(secondShipmentJson.get("trackingNumber").asText()).isEqualTo("BD99999");

        MvcResult detailResult = adminDetail(admin, orderId);
        JsonNode detailJson = objectMapper.readTree(detailResult.getResponse().getContentAsString());
        assertThat(detailJson.get("shipment").get("trackingNumber").asText()).isEqualTo("BD99999");
        assertThat(detailJson.get("notes")).hasSize(1);
    }
}
