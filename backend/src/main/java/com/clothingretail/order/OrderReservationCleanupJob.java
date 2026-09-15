package com.clothingretail.order;

import com.clothingretail.product.ProductVariant;
import com.clothingretail.product.repository.ProductVariantRepository;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Every 60s, finds PENDING_PAYMENT orders whose reservation window has passed, releases each
 * line item's stock reservation via the same atomic conditional-UPDATE used everywhere else in
 * this module, and cancels the order. Nothing was ever decremented for a PENDING_PAYMENT order
 * (only a successful payment decrements stockQuantity), so there's nothing to reverse - only the
 * reservation needs releasing.
 */
@Component
public class OrderReservationCleanupJob {

    private static final Logger log = LoggerFactory.getLogger(OrderReservationCleanupJob.class);

    private final OrderRepository orderRepository;
    private final ProductVariantRepository productVariantRepository;
    private final OrderStatusHistoryService orderStatusHistoryService;

    public OrderReservationCleanupJob(
            OrderRepository orderRepository,
            ProductVariantRepository productVariantRepository,
            OrderStatusHistoryService orderStatusHistoryService) {
        this.orderRepository = orderRepository;
        this.productVariantRepository = productVariantRepository;
        this.orderStatusHistoryService = orderStatusHistoryService;
    }

    @Scheduled(fixedDelay = 60000)
    @Transactional
    public void releaseExpiredReservations() {
        List<Order> expired = orderRepository.findByStatusAndReservationExpiresAtBefore(OrderStatus.PENDING_PAYMENT, Instant.now());
        if (expired.isEmpty()) {
            return;
        }
        for (Order order : expired) {
            for (OrderItem item : order.getItems()) {
                ProductVariant variant = item.getProductVariant();
                if (variant == null) {
                    // Variant was deleted after the order was placed - nothing left to release against.
                    continue;
                }
                productVariantRepository.releaseReservation(variant.getId(), item.getQuantity());
            }
            order.setStatus(OrderStatus.CANCELLED);
            orderStatusHistoryService.record(
                    order, OrderStatus.PENDING_PAYMENT, OrderStatus.CANCELLED, null, "Reservation expired");
            log.info("Released expired reservation and cancelled order {} ({})", order.getId(), order.getOrderNumber());
        }
        orderRepository.saveAll(expired);
    }
}
