package com.clothingretail.order;

import com.clothingretail.cart.Cart;
import com.clothingretail.cart.CartItem;
import com.clothingretail.cart.repository.CartRepository;
import com.clothingretail.common.BadRequestException;
import com.clothingretail.common.ConflictException;
import com.clothingretail.common.NotFoundException;
import com.clothingretail.customer.Address;
import com.clothingretail.customer.repository.AddressRepository;
import com.clothingretail.customer.CustomerProfile;
import com.clothingretail.customer.repository.CustomerProfileRepository;
import com.clothingretail.order.dto.CreateOrderRequest;
import com.clothingretail.product.MediaType;
import com.clothingretail.product.ProductImage;
import com.clothingretail.product.ProductStatus;
import com.clothingretail.product.ProductVariant;
import com.clothingretail.product.ProductVariantRepository;
import com.clothingretail.tax.TaxSettings;
import com.clothingretail.tax.repository.TaxSettingsRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Holds the single {@code @Transactional} method that actually creates an order - kept in its
 * own bean (rather than a private method on {@link OrderService}) specifically so that
 * {@code OrderService.createOrder}'s try/catch around
 * {@link org.springframework.dao.DataIntegrityViolationException} works correctly: calling a
 * method on {@code this} from within the same class bypasses Spring's transactional proxy
 * entirely (the classic "self-invocation" trap), which would silently turn the inner call into a
 * non-transactional one. Routing through a second bean forces the call through the proxy, so the
 * transaction (and its rollback-on-exception behaviour) is real.
 *
 * Pure DB work only, as required: validate -> loop reserve -> create Order+OrderItems -> clear
 * cart -> return. No payment-gateway or other external/slow call ever happens in here.
 */
@Service
@Log4j2
class OrderCreationService {

    private final CustomerProfileRepository customerProfileRepository;
    private final AddressRepository addressRepository;
    private final CartRepository cartRepository;
    private final ProductVariantRepository productVariantRepository;
    private final OrderRepository orderRepository;
    private final OrderStatusHistoryService orderStatusHistoryService;
    private final TaxSettingsRepository taxSettingsRepository;
    private final int reservationTtlMinutes;

    OrderCreationService(
            CustomerProfileRepository customerProfileRepository,
            AddressRepository addressRepository,
            CartRepository cartRepository,
            ProductVariantRepository productVariantRepository,
            OrderRepository orderRepository,
            OrderStatusHistoryService orderStatusHistoryService,
            TaxSettingsRepository taxSettingsRepository,
            @Value("${app.order.reservation-ttl-minutes:15}") int reservationTtlMinutes) {
        this.customerProfileRepository = customerProfileRepository;
        this.addressRepository = addressRepository;
        this.cartRepository = cartRepository;
        this.productVariantRepository = productVariantRepository;
        this.orderRepository = orderRepository;
        this.orderStatusHistoryService = orderStatusHistoryService;
        this.taxSettingsRepository = taxSettingsRepository;
        this.reservationTtlMinutes = reservationTtlMinutes;
    }

    @Transactional
    Order create(Long userId, CreateOrderRequest request) {
        log.info("[1600] Creating order for userId={}, shippingAddressId={}, idempotencyKey={}",
                userId, request.shippingAddressId(), request.idempotencyKey());
        CustomerProfile profile = customerProfileRepository.findByUserId(userId)
                .orElseThrow(() -> {
                    log.error("[1601] Customer profile not found for userId={}", userId);
                    return new NotFoundException("Customer profile not found for user: " + userId);
                });

        Address address = addressRepository.findByIdAndCustomerProfileId(request.shippingAddressId(), profile.getId())
                .orElseThrow(() -> {
                    log.error("[1602] Address {} not found for customerProfileId={}", request.shippingAddressId(), profile.getId());
                    return new NotFoundException("Address not found: " + request.shippingAddressId());
                });

        Cart cart = cartRepository.findByCustomerProfileId(profile.getId())
                .orElseThrow(() -> {
                    log.error("[1603] Cart not found for customerProfileId={}", profile.getId());
                    return new BadRequestException("Cart is empty");
                });
        if (cart.getItems().isEmpty()) {
            log.error("[1604] Cart is empty for customerProfileId={}", profile.getId());
            throw new BadRequestException("Cart is empty");
        }

        Order order = new Order();
        order.setOrderNumber(generateOrderNumber());
        order.setCustomerProfile(profile);
        order.setStatus(OrderStatus.PENDING_PAYMENT);
        order.setIdempotencyKey(request.idempotencyKey());
        order.setShippingAddressLine1(address.getAddressLine1());
        order.setShippingAddressLine2(address.getAddressLine2());
        order.setShippingCity(address.getCity());
        order.setShippingState(address.getState());
        order.setShippingPostalCode(address.getPostalCode());
        order.setShippingCountry(address.getCountry());
        order.setContactName(profile.getUser().getFullName());
        order.setContactPhone(hasText(request.contactPhone()) ? request.contactPhone() : profile.getUser().getMobileNumber());
        order.setShippingCharge(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        // Reservation TTL - hold released by OrderReservationCleanupJob if payment never completes in time.
        order.setReservationExpiresAt(Instant.now().plus(reservationTtlMinutes, ChronoUnit.MINUTES));
        log.info("[1605] Order shell prepared: orderNumber={}, customerProfileId={}, reservationExpiresAt={}",
                order.getOrderNumber(), profile.getId(), order.getReservationExpiresAt());

        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal total = BigDecimal.ZERO;

        // All-or-nothing: every item's reservation happens in this one not-yet-committed
        // transaction. The first one to fail throws immediately, which rolls back everything
        // reserved so far in this same loop - no partial reservation is ever visible to any
        // other transaction.
        for (CartItem cartItem : cart.getItems()) {
            ProductVariant variant = cartItem.getProductVariant();
            boolean productActive = variant.getProduct().getStatus() == ProductStatus.ACTIVE;
            if (!variant.isActive() || !productActive) {
                log.error("[1606] Variant {} unavailable for order: variantActive={}, productActive={}",
                        variant.getSku(), variant.isActive(), productActive);
                throw new ConflictException("'" + variant.getSku() + "' is no longer available");
            }

            int quantity = cartItem.getQuantity();
            int affected = productVariantRepository.reserveStock(variant.getId(), quantity);
            if (affected == 0) {
                log.error("[1607] Stock reservation failed for variant {} (sku={}): requested={}, available={}",
                        variant.getId(), variant.getSku(), quantity, variant.getAvailableQuantity());
                throw new ConflictException(
                        "'" + variant.getProduct().getName() + "' (" + variant.getSku() + ") is no longer available "
                                + "in the requested quantity (" + quantity + ") - only " + variant.getAvailableQuantity()
                                + " left");
            }
            log.info("[1608] Reserved stock for variant {} (sku={}): quantity={}", variant.getId(), variant.getSku(), quantity);

            BigDecimal unitPrice = variant.getSellingPrice();
            BigDecimal discountPercent = variant.getDiscountPercent();
            BigDecimal lineTotal = lineTotal(unitPrice, discountPercent, quantity);

            OrderItem item = new OrderItem();
            item.setProductVariant(variant);
            item.setProductName(variant.getProduct().getName());
            item.setSku(variant.getSku());
            item.setColorName(variant.getColor().getName());
            item.setSizeName(variant.getSize().getName());
            item.setQuantity(quantity);
            item.setUnitPrice(unitPrice);
            item.setDiscountPercent(discountPercent);
            item.setLineTotal(lineTotal);
            item.setImageUrl(primaryImageUrl(variant));
            order.addItem(item);

            subtotal = subtotal.add(unitPrice.multiply(BigDecimal.valueOf(quantity)));
            total = total.add(lineTotal);
        }

        subtotal = subtotal.setScale(2, RoundingMode.HALF_UP);
        total = total.setScale(2, RoundingMode.HALF_UP);
        order.setSubtotal(subtotal);
        order.setDiscountTotal(subtotal.subtract(total).setScale(2, RoundingMode.HALF_UP));

        // GST, snapshotted from the live admin-configured rate - see TaxSettings' own doc comment
        // on why the rate AND the amount it produced are both stored on the order rather than just
        // referencing the (possibly since-changed) settings row. Computed on the goods total after
        // discount, before shipping - "for each product", not on the shipping charge itself.
        TaxSettings taxSettings = taxSettingsRepository.findById(TaxSettings.SINGLETON_ID)
                .orElseThrow(() -> {
                    log.error("[1996] Singleton tax_settings row (id={}) is missing", TaxSettings.SINGLETON_ID);
                    return new IllegalStateException(
                            "Singleton tax_settings row (id=1) is missing - this is a startup-time misconfiguration, "
                                    + "check that V24__tax_settings.sql ran");
                });
        BigDecimal cgstAmount = total.multiply(taxSettings.getCgstPercent())
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal sgstAmount = total.multiply(taxSettings.getSgstPercent())
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        order.setCgstPercent(taxSettings.getCgstPercent());
        order.setCgstAmount(cgstAmount);
        order.setSgstPercent(taxSettings.getSgstPercent());
        order.setSgstAmount(sgstAmount);

        order.setTotalAmount(total.add(order.getShippingCharge()).add(cgstAmount).add(sgstAmount));
        log.info(
                "[1609] Order totals computed: subtotal={}, discountTotal={}, cgstPercent={}, cgstAmount={}, sgstPercent={}, sgstAmount={}, totalAmount={}",
                subtotal, order.getDiscountTotal(), order.getCgstPercent(), cgstAmount, order.getSgstPercent(), sgstAmount, order.getTotalAmount());

        // save() flushes immediately (IDENTITY generation needs the id right away), so a unique
        // constraint violation on idempotency_key from a concurrently-racing identical request
        // surfaces here, inside this transaction, and propagates as a DataIntegrityViolationException
        // to OrderService.createOrder's catch block.
        Order saved = orderRepository.save(order);
        log.info("[1610] Order created: orderId={}, orderNumber={}, status={}", saved.getId(), saved.getOrderNumber(), saved.getStatus());
        // previousStatus is null - this is the order's first-ever status row.
        orderStatusHistoryService.record(saved, null, OrderStatus.PENDING_PAYMENT, null, null);

        cart.getItems().clear();
        cartRepository.save(cart);
        log.info("[1611] Cart cleared for customerProfileId={} after order {}", profile.getId(), saved.getId());

        return saved;
    }

    /** Same resolution ProductService.toSummary() uses for a product's primary image, applied to a single variant: the image marked primary, or its lowest displayOrder one, or null if it has none. */
    private String primaryImageUrl(ProductVariant variant) {
        // Order line-item snapshot is rendered as a plain <img> - a video can't back it, so
        // pick the first still image even if a video happens to be marked primary.
        return variant.getImages().stream()
                .sorted(ProductImage.displayOrderComparator())
                .filter(img -> img.getMediaType() == MediaType.IMAGE)
                .map(ProductImage::getUrl)
                .findFirst()
                .orElse(null);
    }

    private String generateOrderNumber() {
        return "ORD-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private BigDecimal lineTotal(BigDecimal unitPrice, BigDecimal discountPercent, int quantity) {
        BigDecimal gross = unitPrice.multiply(BigDecimal.valueOf(quantity));
        BigDecimal discountFraction = discountPercent.divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP);
        BigDecimal net = gross.multiply(BigDecimal.ONE.subtract(discountFraction));
        return net.setScale(2, RoundingMode.HALF_UP);
    }
}
