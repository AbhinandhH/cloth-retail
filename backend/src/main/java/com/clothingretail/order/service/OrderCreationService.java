package com.clothingretail.order.service;

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
import com.clothingretail.order.Order;
import com.clothingretail.order.OrderItem;
import com.clothingretail.order.OrderStatus;
import com.clothingretail.order.dto.CreateOrderRequest;
import com.clothingretail.order.repository.OrderRepository;
import com.clothingretail.product.MediaType;
import com.clothingretail.product.ProductImage;
import com.clothingretail.product.ProductStatus;
import com.clothingretail.product.ProductVariant;
import com.clothingretail.product.repository.ProductVariantRepository;
import com.clothingretail.siteconfig.SiteConfiguration;
import com.clothingretail.siteconfig.repository.SiteConfigurationRepository;
import com.clothingretail.tax.TaxSettings;
import com.clothingretail.tax.repository.TaxSettingsRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.extern.log4j.Log4j2;
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
 * Pure DB work only, as required: validate -> loop reserve -> create Order+OrderItems -> return.
 * No payment-gateway or other external/slow call ever happens in here.
 *
 * The cart is deliberately NOT cleared here: a freshly-created order is only PENDING_PAYMENT, not
 * a guaranteed sale, so its cart line(s) must survive a failed/expired payment untouched - see
 * {@code OrderItem.sourceCartItemId} and {@code PaymentWebhookServiceImpl#applyOutcome}, which
 * removes exactly the ordered line(s) once (and only once) payment actually succeeds.
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
    private final SiteConfigurationRepository siteConfigurationRepository;

    OrderCreationService(
            CustomerProfileRepository customerProfileRepository,
            AddressRepository addressRepository,
            CartRepository cartRepository,
            ProductVariantRepository productVariantRepository,
            OrderRepository orderRepository,
            OrderStatusHistoryService orderStatusHistoryService,
            TaxSettingsRepository taxSettingsRepository,
            SiteConfigurationRepository siteConfigurationRepository) {
        this.customerProfileRepository = customerProfileRepository;
        this.addressRepository = addressRepository;
        this.cartRepository = cartRepository;
        this.productVariantRepository = productVariantRepository;
        this.orderRepository = orderRepository;
        this.orderStatusHistoryService = orderStatusHistoryService;
        this.taxSettingsRepository = taxSettingsRepository;
        this.siteConfigurationRepository = siteConfigurationRepository;
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
        List<CartItem> itemsToOrder = resolveItemsToOrder(cart, request.cartItemIds(), profile.getId());
        if (itemsToOrder.isEmpty()) {
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
        // Reservation TTL - hold released by OrderReservationCleanupJob if payment never completes
        // in time. Read fresh from SiteConfiguration on every order (not injected once at startup
        // via @Value) so an admin's change on the Site Configuration screen takes effect
        // immediately for new orders, no redeploy needed - see SiteConfiguration's own doc comment
        // on this field.
        Optional<SiteConfiguration> siteConfiguration = siteConfigurationRepository.findById(SiteConfiguration.SINGLETON_ID);
        int reservationTtlMinutes = siteConfiguration
                .map(SiteConfiguration::getOrderReservationTtlMinutes)
                .orElse(15);
        boolean deferReservation = siteConfiguration
                .map(SiteConfiguration::getReserveStockOnlyAtPayment)
                .orElse(false);
        order.setReservationExpiresAt(Instant.now().plus(reservationTtlMinutes, ChronoUnit.MINUTES));
        order.setStockReserved(!deferReservation);
        log.info("[1605] Order shell prepared: orderNumber={}, customerProfileId={}, reservationExpiresAt={}, deferReservation={}",
                order.getOrderNumber(), profile.getId(), order.getReservationExpiresAt(), deferReservation);

        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal total = BigDecimal.ZERO;

        // All-or-nothing: every item's reservation happens in this one not-yet-committed
        // transaction. The first one to fail throws immediately, which rolls back everything
        // reserved so far in this same loop - no partial reservation is ever visible to any
        // other transaction.
        for (CartItem cartItem : itemsToOrder) {
            ProductVariant variant = cartItem.getProductVariant();
            boolean productActive = variant.getProduct().getStatus() == ProductStatus.ACTIVE;
            if (!variant.isActive() || !productActive) {
                log.error("[1606] Variant {} unavailable for order: variantActive={}, productActive={}",
                        variant.getSku(), variant.isActive(), productActive);
                throw new ConflictException("'" + variant.getSku() + "' is no longer available");
            }

            int quantity = cartItem.getQuantity();
            if (deferReservation) {
                log.info("[2000] Deferring stock reservation for variant {} (sku={}): quantity={} - will reserve at Pay-click instead",
                        variant.getId(), variant.getSku(), quantity);
            } else {
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
            }

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
            item.setSourceCartItemId(cartItem.getId());
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
        // referencing the (possibly since-changed) settings row. Product prices are tax-INCLUSIVE
        // (the admin-entered selling price already has GST baked in), so this extracts cgst/sgst
        // out of the post-discount goods total rather than adding them on top of it - a ₹1000 item
        // at 12%+12% yields cgstAmount=sgstAmount≈₹96.77 (₹193.55 total tax "inside" that ₹1000),
        // not ₹1120+. Standard reverse-GST split: each component's share of `total` is
        // total * componentRate / (100 + combinedRate). Computed on the goods total after discount,
        // before shipping - shipping itself carries no tax and is added on top, untaxed, below.
        TaxSettings taxSettings = taxSettingsRepository.findById(TaxSettings.SINGLETON_ID)
                .orElseThrow(() -> {
                    log.error("[1996] Singleton tax_settings row (id={}) is missing", TaxSettings.SINGLETON_ID);
                    return new IllegalStateException(
                            "Singleton tax_settings row (id=1) is missing - this is a startup-time misconfiguration, "
                                    + "check that V24__tax_settings.sql ran");
                });
        BigDecimal combinedRate = taxSettings.getCgstPercent().add(taxSettings.getSgstPercent());
        BigDecimal inclusiveDivisor = BigDecimal.valueOf(100).add(combinedRate);
        BigDecimal cgstAmount = total.multiply(taxSettings.getCgstPercent())
                .divide(inclusiveDivisor, 2, RoundingMode.HALF_UP);
        BigDecimal sgstAmount = total.multiply(taxSettings.getSgstPercent())
                .divide(inclusiveDivisor, 2, RoundingMode.HALF_UP);
        order.setCgstPercent(taxSettings.getCgstPercent());
        order.setCgstAmount(cgstAmount);
        order.setSgstPercent(taxSettings.getSgstPercent());
        order.setSgstAmount(sgstAmount);

        // cgst/sgst are already inside `total` (tax-inclusive pricing) - not added again here.
        order.setTotalAmount(total.add(order.getShippingCharge()));
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

        log.info(
                "[1998] Order {} created from {} cart item(s) for customerProfileId={} - cart left intact pending payment outcome",
                saved.getId(), itemsToOrder.size(), profile.getId());

        return saved;
    }

    /**
     * Null/empty {@code requestedCartItemIds} means "the entire cart" (Proceed to Checkout); a
     * non-empty list scopes the order to just those lines (Buy Now), which must each belong to
     * the caller's own cart - a stale/foreign id fails loudly rather than silently ordering
     * something else or the whole cart instead.
     */
    private List<CartItem> resolveItemsToOrder(Cart cart, List<Long> requestedCartItemIds, Long customerProfileId) {
        if (requestedCartItemIds == null || requestedCartItemIds.isEmpty()) {
            return cart.getItems();
        }
        Map<Long, CartItem> byId = cart.getItems().stream().collect(Collectors.toMap(CartItem::getId, ci -> ci));
        List<CartItem> resolved = new ArrayList<>();
        for (Long id : requestedCartItemIds) {
            CartItem item = byId.get(id);
            if (item == null) {
                log.error("[1999] Buy-now checkout failed - cart item {} not found in cart for customerProfileId={}", id, customerProfileId);
                throw new NotFoundException("Cart item not found: " + id);
            }
            resolved.add(item);
        }
        return resolved;
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
