package com.clothingretail.order;

import static org.assertj.core.api.Assertions.assertThat;

import com.clothingretail.inventory.InventoryTransaction;
import com.clothingretail.inventory.InventoryTransactionRepository;
import com.clothingretail.inventory.InventoryTransactionType;
import com.clothingretail.masterdata.Color;
import com.clothingretail.masterdata.repository.ColorRepository;
import com.clothingretail.masterdata.Size;
import com.clothingretail.masterdata.repository.SizeRepository;
import com.clothingretail.payment.Payment;
import com.clothingretail.payment.PaymentRepository;
import com.clothingretail.payment.PaymentStatus;
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
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * End to end coverage of the full checkout flow: cart -> order (stock reservation) -> payment
 * (fulfil/release). This is the highest-stakes correctness surface in the app, so each test
 * verifies the actual database state (stockQuantity/reservedQuantity/InventoryTransaction), not
 * just HTTP response shapes.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OrderPaymentIntegrationTest {

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
    private OrderRepository orderRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    private ProductVariant createTestVariant(String tag, int stockQuantity) {
        Product product = productRepository.findBySlugAndStatus("floral-straight-kurti", ProductStatus.ACTIVE).orElseThrow();
        Size size = sizeRepository.findByActiveTrueOrderByDisplayOrderAscNameAsc().get(0);
        Color color = colorRepository.findByActiveTrueOrderByDisplayOrderAscNameAsc().get(0);

        ProductVariant variant = new ProductVariant();
        variant.setProduct(product);
        variant.setSku("OPT-" + tag + "-" + System.nanoTime());
        variant.setSize(size);
        variant.setColor(color);
        variant.setSellingPrice(new BigDecimal("500.00"));
        variant.setDiscountPercent(new BigDecimal("10.00"));
        variant.setStockQuantity(stockQuantity);
        variant.setActive(true);
        return productVariantRepository.save(variant);
    }

    /** Registers a customer, gives them a default address, and adds one line item to their cart. */
    private CustomerSession setUpCustomerWithCartItem(String tag, ProductVariant variant, int quantity) throws Exception {
        CustomerSession session = CheckoutTestSupport.registerCustomer(mockMvc, objectMapper, tag);
        CheckoutTestSupport.addToCart(mockMvc, session.accessToken(), variant.getId(), quantity);
        return session;
    }

    private Long createOrderExpectOk(CustomerSession session, Long addressId, String idempotencyKey) throws Exception {
        MvcResult result = CheckoutTestSupport.createOrder(mockMvc, session.accessToken(), idempotencyKey, addressId);
        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    @Test
    void successfulOrderThenPaymentSuccessDecrementsStockAndWritesSaleOutTransaction() throws Exception {
        ProductVariant variant = createTestVariant("SUCCESS", 20);
        CustomerSession session = setUpCustomerWithCartItem("success", variant, 3);
        Long addressId = CheckoutTestSupport.createDefaultAddress(mockMvc, objectMapper, session.accessToken());

        Long orderId = createOrderExpectOk(session, addressId, UUID.randomUUID().toString());

        ProductVariant afterReserve = productVariantRepository.findById(variant.getId()).orElseThrow();
        assertThat(afterReserve.getStockQuantity()).isEqualTo(20);
        assertThat(afterReserve.getReservedQuantity()).isEqualTo(3);

        MvcResult initiateResult = CheckoutTestSupport.initiatePayment(mockMvc, session.accessToken(), orderId);
        assertThat(initiateResult.getResponse().getStatus()).isEqualTo(200);
        JsonNode initiateJson = objectMapper.readTree(initiateResult.getResponse().getContentAsString());
        String gatewayReference = initiateJson.get("gatewayReference").asText();

        MvcResult simulateResult = CheckoutTestSupport.simulatePayment(mockMvc, session.accessToken(), gatewayReference, "SUCCESS");
        assertThat(simulateResult.getResponse().getStatus()).isEqualTo(200);
        JsonNode simulateJson = objectMapper.readTree(simulateResult.getResponse().getContentAsString());
        assertThat(simulateJson.get("orderStatus").asText()).isEqualTo("CONFIRMED");
        assertThat(simulateJson.get("paymentStatus").asText()).isEqualTo("SUCCESS");

        ProductVariant afterPayment = productVariantRepository.findById(variant.getId()).orElseThrow();
        assertThat(afterPayment.getStockQuantity()).isEqualTo(17);
        assertThat(afterPayment.getReservedQuantity()).isEqualTo(0);

        List<InventoryTransaction> transactions = inventoryTransactionRepository.findByProductVariantId(variant.getId());
        InventoryTransaction saleOut = transactions.stream()
                .filter(t -> t.getType() == InventoryTransactionType.SALE_OUT)
                .findFirst()
                .orElseThrow(() -> new AssertionError("No SALE_OUT transaction found"));
        assertThat(saleOut.getQuantity()).isEqualTo(3);
        assertThat(saleOut.getPreviousQuantity()).isEqualTo(20);
        assertThat(saleOut.getNewQuantity()).isEqualTo(17);
        assertThat(saleOut.getReferenceType()).isEqualTo("ORDER");
        assertThat(saleOut.getReferenceId()).isEqualTo(orderId);

        Order order = orderRepository.findById(orderId).orElseThrow();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
    }

    @Test
    void paymentFailureReleasesReservationWithoutTouchingStock() throws Exception {
        ProductVariant variant = createTestVariant("FAIL", 10);
        CustomerSession session = setUpCustomerWithCartItem("fail", variant, 4);
        Long addressId = CheckoutTestSupport.createDefaultAddress(mockMvc, objectMapper, session.accessToken());

        Long orderId = createOrderExpectOk(session, addressId, UUID.randomUUID().toString());

        MvcResult initiateResult = CheckoutTestSupport.initiatePayment(mockMvc, session.accessToken(), orderId);
        String gatewayReference = objectMapper.readTree(initiateResult.getResponse().getContentAsString()).get("gatewayReference").asText();

        MvcResult simulateResult = CheckoutTestSupport.simulatePayment(mockMvc, session.accessToken(), gatewayReference, "FAILURE");
        JsonNode simulateJson = objectMapper.readTree(simulateResult.getResponse().getContentAsString());
        assertThat(simulateJson.get("orderStatus").asText()).isEqualTo("PAYMENT_FAILED");
        assertThat(simulateJson.get("paymentStatus").asText()).isEqualTo("FAILED");

        ProductVariant after = productVariantRepository.findById(variant.getId()).orElseThrow();
        assertThat(after.getStockQuantity()).isEqualTo(10);
        assertThat(after.getReservedQuantity()).isEqualTo(0);

        boolean anySaleOut = inventoryTransactionRepository.findByProductVariantId(variant.getId()).stream()
                .anyMatch(t -> t.getType() == InventoryTransactionType.SALE_OUT);
        assertThat(anySaleOut).isFalse();
    }

    @Test
    void insufficientStockAtOrderCreationLeavesCartAndStockUntouched() throws Exception {
        // Models a real race: both customers add the item to their cart while it's still fully
        // available, then customer A checks out first (consuming the only 2 units) before
        // customer B gets to checkout - B's order creation must be rejected atomically, not
        // silently succeed or partially reserve.
        ProductVariant variant = createTestVariant("INSUFFICIENT", 2);
        CustomerSession sessionB = setUpCustomerWithCartItem("insufficientB", variant, 2);
        Long addressB = CheckoutTestSupport.createDefaultAddress(mockMvc, objectMapper, sessionB.accessToken());

        CustomerSession sessionA = setUpCustomerWithCartItem("insufficientA", variant, 2);
        Long addressA = CheckoutTestSupport.createDefaultAddress(mockMvc, objectMapper, sessionA.accessToken());

        // A checks out first, taking both available units.
        Long firstOrderId = createOrderExpectOk(sessionA, addressA, UUID.randomUUID().toString());
        assertThat(firstOrderId).isNotNull();

        // B's cart still holds 2 units of a variant that now has 0 available - order creation
        // must reject the whole thing atomically (see OrderCreationService.create).
        MvcResult rejected = CheckoutTestSupport.createOrder(mockMvc, sessionB.accessToken(), UUID.randomUUID().toString(), addressB);
        assertThat(rejected.getResponse().getStatus()).isEqualTo(409);

        // B's cart item is untouched (order creation rolled back entirely, nothing was cleared).
        MvcResult cartResult = CheckoutTestSupport.getCart(mockMvc, sessionB.accessToken());
        JsonNode cartJson = objectMapper.readTree(cartResult.getResponse().getContentAsString());
        assertThat(cartJson.get("items").size()).isEqualTo(1);
        assertThat(cartJson.get("items").get(0).get("quantity").asInt()).isEqualTo(2);

        // Only A's reservation exists - B's rejected attempt reserved nothing.
        ProductVariant afterRejection = productVariantRepository.findById(variant.getId()).orElseThrow();
        assertThat(afterRejection.getReservedQuantity()).isEqualTo(2);
        assertThat(afterRejection.getAvailableQuantity()).isEqualTo(0);
    }

    @Test
    void duplicateIdempotencyKeyReturnsSameOrderWithoutDoubleReserving() throws Exception {
        ProductVariant variant = createTestVariant("IDEMPOTENT", 15);
        CustomerSession session = setUpCustomerWithCartItem("idempotent", variant, 5);
        Long addressId = CheckoutTestSupport.createDefaultAddress(mockMvc, objectMapper, session.accessToken());
        String idempotencyKey = UUID.randomUUID().toString();

        Long firstOrderId = createOrderExpectOk(session, addressId, idempotencyKey);

        // Cart was cleared by the first call - re-add the same item so a second identical
        // request has something to (attempt to) reserve again, if the idempotency guard failed.
        CheckoutTestSupport.addToCart(mockMvc, session.accessToken(), variant.getId(), 5);
        MvcResult secondResult = CheckoutTestSupport.createOrder(mockMvc, session.accessToken(), idempotencyKey, addressId);
        assertThat(secondResult.getResponse().getStatus()).isEqualTo(200);
        Long secondOrderId = objectMapper.readTree(secondResult.getResponse().getContentAsString()).get("id").asLong();

        assertThat(secondOrderId).isEqualTo(firstOrderId);

        ProductVariant after = productVariantRepository.findById(variant.getId()).orElseThrow();
        assertThat(after.getReservedQuantity()).isEqualTo(5);

        assertThat(orderRepository.findByIdempotencyKey(idempotencyKey)).isPresent();
        assertThat(orderRepository.findAll().stream().filter(o -> o.getIdempotencyKey().equals(idempotencyKey)).count()).isEqualTo(1);
    }

    @Test
    void simulatingSameOutcomeTwiceIsANoOpTheSecondTime() throws Exception {
        ProductVariant variant = createTestVariant("DOUBLEWEBHOOK", 8);
        CustomerSession session = setUpCustomerWithCartItem("doublewebhook", variant, 2);
        Long addressId = CheckoutTestSupport.createDefaultAddress(mockMvc, objectMapper, session.accessToken());
        Long orderId = createOrderExpectOk(session, addressId, UUID.randomUUID().toString());

        MvcResult initiateResult = CheckoutTestSupport.initiatePayment(mockMvc, session.accessToken(), orderId);
        String gatewayReference = objectMapper.readTree(initiateResult.getResponse().getContentAsString()).get("gatewayReference").asText();

        CheckoutTestSupport.simulatePayment(mockMvc, session.accessToken(), gatewayReference, "SUCCESS");
        ProductVariant afterFirst = productVariantRepository.findById(variant.getId()).orElseThrow();
        int stockAfterFirst = afterFirst.getStockQuantity();

        MvcResult secondSimulate = CheckoutTestSupport.simulatePayment(mockMvc, session.accessToken(), gatewayReference, "SUCCESS");
        assertThat(secondSimulate.getResponse().getStatus()).isEqualTo(200);
        JsonNode secondJson = objectMapper.readTree(secondSimulate.getResponse().getContentAsString());
        assertThat(secondJson.get("orderStatus").asText()).isEqualTo("CONFIRMED");
        assertThat(secondJson.get("paymentStatus").asText()).isEqualTo("SUCCESS");

        ProductVariant afterSecond = productVariantRepository.findById(variant.getId()).orElseThrow();
        assertThat(afterSecond.getStockQuantity()).isEqualTo(stockAfterFirst);

        long saleOutCount = inventoryTransactionRepository.findByProductVariantId(variant.getId()).stream()
                .filter(t -> t.getType() == InventoryTransactionType.SALE_OUT)
                .count();
        assertThat(saleOutCount).isEqualTo(1);

        Payment payment = paymentRepository.findByGatewayReference(gatewayReference).orElseThrow();
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
    }

    @Test
    void customerCannotViewAnotherCustomersOrder() throws Exception {
        ProductVariant variant = createTestVariant("OWNERSHIP", 6);
        CustomerSession owner = setUpCustomerWithCartItem("owner", variant, 1);
        Long addressId = CheckoutTestSupport.createDefaultAddress(mockMvc, objectMapper, owner.accessToken());
        Long orderId = createOrderExpectOk(owner, addressId, UUID.randomUUID().toString());

        CustomerSession intruder = CheckoutTestSupport.registerCustomer(mockMvc, objectMapper, "intruder");
        MvcResult result = CheckoutTestSupport.getOrder(mockMvc, intruder.accessToken(), orderId);
        assertThat(result.getResponse().getStatus()).isEqualTo(404);
    }
}
