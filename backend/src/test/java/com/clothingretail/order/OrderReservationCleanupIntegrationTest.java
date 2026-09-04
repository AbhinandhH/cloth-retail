package com.clothingretail.order;

import static org.assertj.core.api.Assertions.assertThat;

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
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Verifies {@link OrderReservationCleanupJob} directly (calling its method in-process rather than
 * waiting out the real 60s @Scheduled delay): an order whose reservation has expired gets
 * cancelled and its stock reservation released.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OrderReservationCleanupIntegrationTest {

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
    private OrderRepository orderRepository;

    @Autowired
    private OrderReservationCleanupJob cleanupJob;

    private ProductVariant createTestVariant(int stockQuantity) {
        Product product = productRepository.findBySlugAndStatus("floral-straight-kurti", ProductStatus.ACTIVE).orElseThrow();
        Size size = sizeRepository.findByActiveTrueOrderByDisplayOrderAscNameAsc().get(0);
        Color color = colorRepository.findByActiveTrueOrderByDisplayOrderAscNameAsc().get(0);

        ProductVariant variant = new ProductVariant();
        variant.setProduct(product);
        variant.setSku("EXPIRE-" + System.nanoTime());
        variant.setSize(size);
        variant.setColor(color);
        variant.setSellingPrice(new BigDecimal("300.00"));
        variant.setDiscountPercent(BigDecimal.ZERO);
        variant.setStockQuantity(stockQuantity);
        variant.setActive(true);
        return productVariantRepository.save(variant);
    }

    @Test
    void expiredPendingPaymentOrderIsCancelledAndReservationReleased() throws Exception {
        ProductVariant variant = createTestVariant(9);
        CustomerSession session = CheckoutTestSupport.registerCustomer(mockMvc, objectMapper, "expiry");
        CheckoutTestSupport.addToCart(mockMvc, session.accessToken(), variant.getId(), 4);
        Long addressId = CheckoutTestSupport.createDefaultAddress(mockMvc, objectMapper, session.accessToken());

        MvcResult orderResult = CheckoutTestSupport.createOrder(mockMvc, session.accessToken(), UUID.randomUUID().toString(), addressId);
        assertThat(orderResult.getResponse().getStatus()).isEqualTo(200);
        Long orderId = objectMapper.readTree(orderResult.getResponse().getContentAsString()).get("id").asLong();

        ProductVariant afterReserve = productVariantRepository.findById(variant.getId()).orElseThrow();
        assertThat(afterReserve.getReservedQuantity()).isEqualTo(4);

        // Force the reservation into the past instead of waiting out the real TTL.
        Order order = orderRepository.findById(orderId).orElseThrow();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING_PAYMENT);
        order.setReservationExpiresAt(Instant.now().minus(1, ChronoUnit.HOURS));
        orderRepository.save(order);

        cleanupJob.releaseExpiredReservations();

        Order afterCleanup = orderRepository.findById(orderId).orElseThrow();
        assertThat(afterCleanup.getStatus()).isEqualTo(OrderStatus.CANCELLED);

        ProductVariant afterRelease = productVariantRepository.findById(variant.getId()).orElseThrow();
        assertThat(afterRelease.getReservedQuantity()).isEqualTo(0);
        assertThat(afterRelease.getStockQuantity()).isEqualTo(9);
    }

    @Test
    void nonExpiredOrderIsLeftAlone() throws Exception {
        ProductVariant variant = createTestVariant(5);
        CustomerSession session = CheckoutTestSupport.registerCustomer(mockMvc, objectMapper, "notyet");
        CheckoutTestSupport.addToCart(mockMvc, session.accessToken(), variant.getId(), 1);
        Long addressId = CheckoutTestSupport.createDefaultAddress(mockMvc, objectMapper, session.accessToken());

        MvcResult orderResult = CheckoutTestSupport.createOrder(mockMvc, session.accessToken(), UUID.randomUUID().toString(), addressId);
        Long orderId = objectMapper.readTree(orderResult.getResponse().getContentAsString()).get("id").asLong();

        cleanupJob.releaseExpiredReservations();

        Order afterCleanup = orderRepository.findById(orderId).orElseThrow();
        assertThat(afterCleanup.getStatus()).isEqualTo(OrderStatus.PENDING_PAYMENT);

        ProductVariant afterRun = productVariantRepository.findById(variant.getId()).orElseThrow();
        assertThat(afterRun.getReservedQuantity()).isEqualTo(1);
    }
}
