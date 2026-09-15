package com.clothingretail.order;

import static org.assertj.core.api.Assertions.assertThat;

import com.clothingretail.inventory.InventoryTransaction;
import com.clothingretail.inventory.repository.InventoryTransactionRepository;
import com.clothingretail.inventory.InventoryTransactionType;
import com.clothingretail.masterdata.Color;
import com.clothingretail.masterdata.repository.ColorRepository;
import com.clothingretail.masterdata.Size;
import com.clothingretail.masterdata.repository.SizeRepository;
import com.clothingretail.order.repository.OrderRepository;
import com.clothingretail.payment.Payment;
import com.clothingretail.payment.repository.PaymentRepository;
import com.clothingretail.payment.PaymentStatus;
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

        // The cart is deliberately left intact by order creation now (see
        // OrderCreationService), so the original line is still sitting there - add another 5 on
        // top of it so a second identical request would have extra stock to (attempt to) reserve
        // again, if the idempotency guard failed.
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
    void cartLineStaysInCartAfterOrderCreationAndIsOnlyRemovedOncePaymentSucceeds() throws Exception {
        ProductVariant variant = createTestVariant("CARTLINGER", 10);
        CustomerSession session = setUpCustomerWithCartItem("cartlingers", variant, 2);
        Long addressId = CheckoutTestSupport.createDefaultAddress(mockMvc, objectMapper, session.accessToken());

        Long orderId = createOrderExpectOk(session, addressId, UUID.randomUUID().toString());

        // PENDING_PAYMENT is not a guaranteed sale yet - the ordered line must still be visible
        // (and editable) in the cart.
        MvcResult cartAfterOrder = CheckoutTestSupport.getCart(mockMvc, session.accessToken());
        JsonNode cartAfterOrderJson = objectMapper.readTree(cartAfterOrder.getResponse().getContentAsString());
        assertThat(cartAfterOrderJson.get("items").size()).isEqualTo(1);
        assertThat(cartAfterOrderJson.get("items").get(0).get("quantity").asInt()).isEqualTo(2);

        MvcResult initiateResult = CheckoutTestSupport.initiatePayment(mockMvc, session.accessToken(), orderId);
        String gatewayReference = objectMapper.readTree(initiateResult.getResponse().getContentAsString()).get("gatewayReference").asText();
        CheckoutTestSupport.simulatePayment(mockMvc, session.accessToken(), gatewayReference, "SUCCESS");

        // Only now, once payment has actually succeeded, is the cart line removed.
        MvcResult cartAfterPayment = CheckoutTestSupport.getCart(mockMvc, session.accessToken());
        JsonNode cartAfterPaymentJson = objectMapper.readTree(cartAfterPayment.getResponse().getContentAsString());
        assertThat(cartAfterPaymentJson.get("items").size()).isEqualTo(0);
    }

    @Test
    void cartLineRemainsInCartWhenPaymentFails() throws Exception {
        ProductVariant variant = createTestVariant("CARTKEEP", 10);
        CustomerSession session = setUpCustomerWithCartItem("cartkeep", variant, 2);
        Long addressId = CheckoutTestSupport.createDefaultAddress(mockMvc, objectMapper, session.accessToken());

        Long orderId = createOrderExpectOk(session, addressId, UUID.randomUUID().toString());

        MvcResult initiateResult = CheckoutTestSupport.initiatePayment(mockMvc, session.accessToken(), orderId);
        String gatewayReference = objectMapper.readTree(initiateResult.getResponse().getContentAsString()).get("gatewayReference").asText();
        CheckoutTestSupport.simulatePayment(mockMvc, session.accessToken(), gatewayReference, "FAILURE");

        MvcResult cartAfterFailure = CheckoutTestSupport.getCart(mockMvc, session.accessToken());
        JsonNode cartAfterFailureJson = objectMapper.readTree(cartAfterFailure.getResponse().getContentAsString());
        assertThat(cartAfterFailureJson.get("items").size()).isEqualTo(1);
        assertThat(cartAfterFailureJson.get("items").get(0).get("quantity").asInt()).isEqualTo(2);
    }

    @Test
    void buyNowScopedOrderLeavesOtherCartLinesUntouchedAndOnlyRemovesTheOrderedOneOnSuccess() throws Exception {
        ProductVariant variantA = createTestVariant("BUYNOWA", 10);
        ProductVariant variantB = createTestVariant("BUYNOWB", 10);
        CustomerSession session = CheckoutTestSupport.registerCustomer(mockMvc, objectMapper, "buynow");
        Long addressId = CheckoutTestSupport.createDefaultAddress(mockMvc, objectMapper, session.accessToken());

        MvcResult addA = CheckoutTestSupport.addToCart(mockMvc, session.accessToken(), variantA.getId(), 1);
        Long cartItemIdA =
                objectMapper.readTree(addA.getResponse().getContentAsString()).get("items").get(0).get("id").asLong();
        CheckoutTestSupport.addToCart(mockMvc, session.accessToken(), variantB.getId(), 1);

        // Buy Now on variant A only - variant B's line must never be reserved or included.
        MvcResult orderResult = CheckoutTestSupport.createOrder(
                mockMvc, session.accessToken(), UUID.randomUUID().toString(), addressId, List.of(cartItemIdA));
        assertThat(orderResult.getResponse().getStatus()).isEqualTo(200);
        JsonNode orderJson = objectMapper.readTree(orderResult.getResponse().getContentAsString());
        assertThat(orderJson.get("items").size()).isEqualTo(1);
        assertThat(orderJson.get("items").get(0).get("sku").asText()).isEqualTo(variantA.getSku());
        Long orderId = orderJson.get("id").asLong();

        ProductVariant variantBAfterOrder = productVariantRepository.findById(variantB.getId()).orElseThrow();
        assertThat(variantBAfterOrder.getReservedQuantity()).isEqualTo(0);

        // Both lines still present right after order creation - Buy Now must not touch the rest
        // of the cart.
        MvcResult cartAfterOrder = CheckoutTestSupport.getCart(mockMvc, session.accessToken());
        JsonNode cartAfterOrderJson = objectMapper.readTree(cartAfterOrder.getResponse().getContentAsString());
        assertThat(cartAfterOrderJson.get("items").size()).isEqualTo(2);

        MvcResult initiateResult = CheckoutTestSupport.initiatePayment(mockMvc, session.accessToken(), orderId);
        String gatewayReference = objectMapper.readTree(initiateResult.getResponse().getContentAsString()).get("gatewayReference").asText();
        CheckoutTestSupport.simulatePayment(mockMvc, session.accessToken(), gatewayReference, "SUCCESS");

        // Only variant A's line was removed on success - variant B is still sitting in the cart,
        // completely untouched throughout.
        MvcResult cartAfterPayment = CheckoutTestSupport.getCart(mockMvc, session.accessToken());
        JsonNode cartAfterPaymentJson = objectMapper.readTree(cartAfterPayment.getResponse().getContentAsString());
        assertThat(cartAfterPaymentJson.get("items").size()).isEqualTo(1);
        assertThat(cartAfterPaymentJson.get("items").get(0).get("sku").asText()).isEqualTo(variantB.getSku());
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
