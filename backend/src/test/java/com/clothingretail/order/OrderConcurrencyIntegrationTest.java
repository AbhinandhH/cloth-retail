package com.clothingretail.order;

import static org.assertj.core.api.Assertions.assertThat;

import com.clothingretail.auth.AuthService;
import com.clothingretail.auth.dto.RegisterRequest;
import com.clothingretail.common.ConflictException;
import com.clothingretail.cart.Cart;
import com.clothingretail.cart.CartItem;
import com.clothingretail.cart.repository.CartRepository;
import com.clothingretail.customer.Address;
import com.clothingretail.customer.AddressRepository;
import com.clothingretail.customer.CustomerProfile;
import com.clothingretail.customer.CustomerProfileRepository;
import com.clothingretail.masterdata.Color;
import com.clothingretail.masterdata.ColorRepository;
import com.clothingretail.masterdata.Size;
import com.clothingretail.masterdata.SizeRepository;
import com.clothingretail.order.dto.CreateOrderRequest;
import com.clothingretail.product.Product;
import com.clothingretail.product.ProductRepository;
import com.clothingretail.product.ProductStatus;
import com.clothingretail.product.ProductVariant;
import com.clothingretail.product.ProductVariantRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * The critical correctness test for this whole module: two REAL concurrent threads race to order
 * the last unit of a variant that has exactly one available. Sequential calls (even back to back)
 * would not exercise the database row-lock behind {@code ProductVariantRepository.reserveStock} -
 * only genuine concurrent execution does, which is why this uses an ExecutorService + a
 * CountDownLatch to force both requests to hit {@code OrderService.createOrder} at (as close to)
 * the same instant as the JVM allows.
 *
 * Drives {@link OrderService} directly (the real, fully-transactional Spring bean) rather than
 * through MockMvc/HTTP - this is exactly the boundary two concurrent HTTP request-handling
 * threads would call into in production, and it removes any doubt about whether MockMvc itself is
 * safe to invoke concurrently.
 */
@SpringBootTest
@ActiveProfiles("test")
class OrderConcurrencyIntegrationTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private AuthService authService;

    @Autowired
    private CustomerProfileRepository customerProfileRepository;

    @Autowired
    private AddressRepository addressRepository;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductVariantRepository productVariantRepository;

    @Autowired
    private SizeRepository sizeRepository;

    @Autowired
    private ColorRepository colorRepository;

    private Long registerCustomerAndGetUserId(String tag) {
        String email = "concurrency-" + tag + "-" + System.nanoTime() + "@example.com";
        RegisterRequest request = new RegisterRequest("Concurrency Tester " + tag, email, "9000000002", "Password123!");
        return authService.register(request).body().auth().user().id();
    }

    private Long createAddressFor(Long userId) {
        CustomerProfile profile = customerProfileRepository.findByUserId(userId).orElseThrow();
        Address address = new Address();
        address.setCustomerProfile(profile);
        address.setLabel("Home");
        address.setAddressLine1("1 Test Lane");
        address.setCity("Mumbai");
        address.setState("Maharashtra");
        address.setPostalCode("400001");
        address.setCountry("India");
        return addressRepository.save(address).getId();
    }

    private void addToCartDirect(Long userId, ProductVariant variant, int quantity) {
        CustomerProfile profile = customerProfileRepository.findByUserId(userId).orElseThrow();
        Cart cart = new Cart(profile);
        CartItem item = new CartItem();
        item.setProductVariant(variant);
        item.setQuantity(quantity);
        cart.addItem(item);
        cartRepository.save(cart);
    }

    private ProductVariant createTestVariant(int stockQuantity) {
        Product product = productRepository.findBySlugAndStatus("floral-straight-kurti", ProductStatus.ACTIVE).orElseThrow();
        Size size = sizeRepository.findByActiveTrueOrderByDisplayOrderAscNameAsc().get(0);
        Color color = colorRepository.findByActiveTrueOrderByDisplayOrderAscNameAsc().get(0);

        ProductVariant variant = new ProductVariant();
        variant.setProduct(product);
        variant.setSku("CONC-" + System.nanoTime());
        variant.setSize(size);
        variant.setColor(color);
        variant.setSellingPrice(new BigDecimal("999.00"));
        variant.setDiscountPercent(BigDecimal.ZERO);
        variant.setStockQuantity(stockQuantity);
        variant.setActive(true);
        return productVariantRepository.save(variant);
    }

    @Test
    void exactlyOneOfTwoConcurrentOrdersForTheLastUnitSucceeds() throws Exception {
        ProductVariant variant = createTestVariant(1);

        Long userA = registerCustomerAndGetUserId("a");
        Long userB = registerCustomerAndGetUserId("b");
        Long addressA = createAddressFor(userA);
        Long addressB = createAddressFor(userB);
        addToCartDirect(userA, variant, 1);
        addToCartDirect(userB, variant, 1);

        CreateOrderRequest requestA = new CreateOrderRequest(UUID.randomUUID().toString(), addressA, "9000000010");
        CreateOrderRequest requestB = new CreateOrderRequest(UUID.randomUUID().toString(), addressB, "9000000011");

        CountDownLatch startLatch = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Callable<Outcome> taskA = () -> attemptOrder(startLatch, userA, requestA);
            Callable<Outcome> taskB = () -> attemptOrder(startLatch, userB, requestB);

            Future<Outcome> futureA = executor.submit(taskA);
            Future<Outcome> futureB = executor.submit(taskB);

            // Both threads are now waiting on the latch (or extremely close to it) - release them together.
            startLatch.countDown();

            Outcome outcomeA = futureA.get(10, TimeUnit.SECONDS);
            Outcome outcomeB = futureB.get(10, TimeUnit.SECONDS);

            List<Outcome> outcomes = List.of(outcomeA, outcomeB);
            long successCount = outcomes.stream().filter(Outcome::succeeded).count();
            long rejectedCount = outcomes.stream().filter(o -> !o.succeeded() && o.wasConflict()).count();

            assertThat(successCount)
                    .as("exactly one of the two concurrent orders for the last unit must succeed")
                    .isEqualTo(1);
            assertThat(rejectedCount)
                    .as("the other must be cleanly rejected as a stock conflict, not fail for some other reason")
                    .isEqualTo(1);
        } finally {
            executor.shutdownNow();
        }

        ProductVariant after = productVariantRepository.findById(variant.getId()).orElseThrow();
        assertThat(after.getStockQuantity()).isEqualTo(1);
        assertThat(after.getReservedQuantity()).isEqualTo(1);
        assertThat(after.getAvailableQuantity()).isEqualTo(0);
    }

    private Outcome attemptOrder(CountDownLatch startLatch, Long userId, CreateOrderRequest request) {
        try {
            startLatch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new Outcome(false, false);
        }
        try {
            orderService.createOrder(userId, request);
            return new Outcome(true, false);
        } catch (ConflictException ex) {
            return new Outcome(false, true);
        }
    }

    private record Outcome(boolean succeeded, boolean wasConflict) {}
}
