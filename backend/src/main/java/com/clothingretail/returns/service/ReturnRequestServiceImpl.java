package com.clothingretail.returns.service;

import com.clothingretail.common.ConflictException;
import com.clothingretail.common.NotFoundException;
import com.clothingretail.common.PageResponse;
import com.clothingretail.customer.CustomerProfile;
import com.clothingretail.customer.repository.CustomerProfileRepository;
import com.clothingretail.order.Order;
import com.clothingretail.order.OrderItem;
import com.clothingretail.order.OrderStatus;
import com.clothingretail.order.repository.OrderRepository;
import com.clothingretail.product.ProductVariant;
import com.clothingretail.product.repository.ProductVariantRepository;
import com.clothingretail.returns.EvidenceStatus;
import com.clothingretail.returns.RequestType;
import com.clothingretail.returns.ReturnRequest;
import com.clothingretail.returns.ReturnRequestStatus;
import com.clothingretail.returns.dto.CreateDamageRequest;
import com.clothingretail.returns.dto.CreateExchangeRequest;
import com.clothingretail.returns.dto.EligibleOrderItemResponse;
import com.clothingretail.returns.dto.ReplacementSizeOption;
import com.clothingretail.returns.dto.ReturnRequestDetailResponse;
import com.clothingretail.returns.dto.ReturnRequestSummaryResponse;
import com.clothingretail.returns.repository.ReturnRequestRepository;
import java.util.List;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Log4j2
public class ReturnRequestServiceImpl implements ReturnRequestService {

    private final ReturnRequestRepository returnRequestRepository;
    private final OrderRepository orderRepository;
    private final CustomerProfileRepository customerProfileRepository;
    private final ProductVariantRepository productVariantRepository;
    private final ReturnEvidenceStorageService evidenceStorageService;
    private final WhatsAppService whatsAppService;

    public ReturnRequestServiceImpl(
            ReturnRequestRepository returnRequestRepository,
            OrderRepository orderRepository,
            CustomerProfileRepository customerProfileRepository,
            ProductVariantRepository productVariantRepository,
            ReturnEvidenceStorageService evidenceStorageService,
            WhatsAppService whatsAppService) {
        this.returnRequestRepository = returnRequestRepository;
        this.orderRepository = orderRepository;
        this.customerProfileRepository = customerProfileRepository;
        this.productVariantRepository = productVariantRepository;
        this.evidenceStorageService = evidenceStorageService;
        this.whatsAppService = whatsAppService;
    }

    @Override
    @Transactional
    public ReturnRequestDetailResponse createExchangeRequest(
            Long userId, Long orderId, Long orderItemId, CreateExchangeRequest request) {
        log.info("[2010] createExchangeRequest requested: userId={}, orderId={}, orderItemId={}, requestedVariantId={}",
                userId, orderId, orderItemId, request.requestedVariantId());
        CustomerProfile profile = resolveProfile(userId);
        Order order = resolveOwnedDeliveredOrder(orderId, profile);
        OrderItem item = resolveOwnedItem(order, orderItemId);
        rejectIfDuplicateActive(orderItemId);

        ProductVariant originalVariant = item.getProductVariant();
        if (originalVariant == null) {
            log.error("[2011] Exchange request rejected: original variant no longer exists, orderItemId={}", orderItemId);
            throw new ConflictException("This item's product is no longer available for exchange");
        }

        ProductVariant requestedVariant = productVariantRepository.findById(request.requestedVariantId())
                .orElseThrow(() -> {
                    log.error("[2012] Exchange request rejected: requested variant not found, id={}", request.requestedVariantId());
                    return new NotFoundException("Requested size not found: " + request.requestedVariantId());
                });
        if (!requestedVariant.getProduct().getId().equals(originalVariant.getProduct().getId())) {
            log.error("[2013] Exchange request rejected: requested variant {} is a different product than original variant {}",
                    requestedVariant.getId(), originalVariant.getId());
            throw new ConflictException("You can only exchange for a different size of the same product");
        }
        if (requestedVariant.getId().equals(originalVariant.getId())) {
            log.error("[2014] Exchange request rejected: requested variant is the same as the original, variantId={}", originalVariant.getId());
            throw new ConflictException("The requested size must be different from the size you already have");
        }
        if (!requestedVariant.isActive() || requestedVariant.getAvailableQuantity() <= 0) {
            log.error("[2015] Exchange request rejected: requested variant {} is not currently available (active={}, availableQuantity={})",
                    requestedVariant.getId(), requestedVariant.isActive(), requestedVariant.getAvailableQuantity());
            throw new ConflictException("The requested size is not currently in stock");
        }

        ReturnRequest returnRequest = new ReturnRequest();
        returnRequest.setOrder(order);
        returnRequest.setOrderItem(item);
        returnRequest.setCustomerProfile(profile);
        returnRequest.setRequestType(RequestType.SIZE_EXCHANGE);
        returnRequest.setStatus(ReturnRequestStatus.PENDING);
        returnRequest.setReason(request.reason());
        returnRequest.setRequestedVariant(requestedVariant);
        returnRequest.setRequestedSizeName(requestedVariant.getSize().getName());
        ReturnRequest saved = returnRequestRepository.save(returnRequest);
        log.info("[2016] Exchange request created: id={}, orderItemId={}, requestedVariantId={}",
                saved.getId(), orderItemId, requestedVariant.getId());

        return toDetailResponse(saved);
    }

    @Override
    @Transactional
    public ReturnRequestDetailResponse createDamageRequest(Long userId, Long orderId, Long orderItemId, CreateDamageRequest request) {
        log.info("[2017] createDamageRequest requested: userId={}, orderId={}, orderItemId={}", userId, orderId, orderItemId);
        CustomerProfile profile = resolveProfile(userId);
        Order order = resolveOwnedDeliveredOrder(orderId, profile);
        OrderItem item = resolveOwnedItem(order, orderItemId);
        rejectIfDuplicateActive(orderItemId);

        String referenceCode = generateUniqueReferenceCode();

        ReturnRequest returnRequest = new ReturnRequest();
        returnRequest.setOrder(order);
        returnRequest.setOrderItem(item);
        returnRequest.setCustomerProfile(profile);
        returnRequest.setRequestType(RequestType.DAMAGED_PRODUCT);
        returnRequest.setStatus(ReturnRequestStatus.PENDING);
        returnRequest.setReason(request.reason());
        returnRequest.setEvidenceStatus(EvidenceStatus.NOT_SUBMITTED);
        returnRequest.setEvidenceReferenceCode(referenceCode);
        ReturnRequest saved = returnRequestRepository.save(returnRequest);
        log.info("[2018] Damage request created: id={}, orderItemId={}, evidenceReferenceCode={}",
                saved.getId(), orderItemId, referenceCode);

        return toDetailResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ReturnRequestSummaryResponse> listMyRequests(Long userId, int page, int size) {
        CustomerProfile profile = resolveProfile(userId);
        Page<ReturnRequest> requests = returnRequestRepository.findByCustomerProfileId(
                profile.getId(), PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        log.info("[2019] Listed {} return requests (of {} total) for userId={}", requests.getNumberOfElements(), requests.getTotalElements(), userId);
        return PageResponse.of(requests, requests.getContent().stream().map(this::toSummaryResponse).toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ReturnRequestDetailResponse getMyRequest(Long userId, Long requestId) {
        CustomerProfile profile = resolveProfile(userId);
        ReturnRequest returnRequest = resolveOwnedRequest(requestId, profile);
        return toDetailResponse(returnRequest);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EligibleOrderItemResponse> listEligibleItems(Long userId, Long orderId) {
        CustomerProfile profile = resolveProfile(userId);
        Order order = resolveOwnedDeliveredOrder(orderId, profile);
        return order.getItems().stream().map(this::toEligibleItemResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public EvidenceFile loadMyEvidence(Long userId, Long requestId) {
        CustomerProfile profile = resolveProfile(userId);
        ReturnRequest returnRequest = resolveOwnedRequest(requestId, profile);
        if (returnRequest.getEvidenceFilename() == null) {
            log.error("[2020] Evidence fetch rejected: no evidence uploaded yet for request {}", requestId);
            throw new NotFoundException("No evidence has been uploaded for this request yet");
        }
        return new EvidenceFile(evidenceStorageService.load(returnRequest.getEvidenceFilename()), returnRequest.getEvidenceContentType());
    }

    private EligibleOrderItemResponse toEligibleItemResponse(OrderItem item) {
        Long activeRequestId = returnRequestRepository
                .findFirstByOrderItemIdAndStatusNot(item.getId(), ReturnRequestStatus.REJECTED)
                .map(ReturnRequest::getId)
                .orElse(null);
        boolean hasActiveRequest = activeRequestId != null;
        List<ReplacementSizeOption> replacementSizes = List.of();
        ProductVariant variant = item.getProductVariant();
        if (variant != null && !hasActiveRequest) {
            replacementSizes = productVariantRepository.findByProductIdAndColorId(variant.getProduct().getId(), variant.getColor().getId())
                    .stream()
                    .filter(v -> v.isActive() && v.getAvailableQuantity() > 0 && !v.getId().equals(variant.getId()))
                    .map(v -> new ReplacementSizeOption(v.getId(), v.getSize().getName(), v.getAvailableQuantity()))
                    .toList();
        }
        return new EligibleOrderItemResponse(
                item.getId(), item.getProductName(), item.getSku(), item.getColorName(), item.getSizeName(),
                item.getQuantity(), item.getImageUrl(), hasActiveRequest, activeRequestId, replacementSizes);
    }

    private void rejectIfDuplicateActive(Long orderItemId) {
        if (returnRequestRepository.existsByOrderItemIdAndStatusNot(orderItemId, ReturnRequestStatus.REJECTED)) {
            log.error("[2021] Request rejected: an active request already exists for orderItemId={}", orderItemId);
            throw new ConflictException("An active return/exchange request already exists for this item");
        }
    }

    private String generateUniqueReferenceCode() {
        // Astronomically unlikely to ever loop more than once (8-char random alphabet), but
        // checked rather than assumed, same defensiveness this codebase uses elsewhere for
        // generated unique values.
        for (int attempt = 0; attempt < 5; attempt++) {
            String code = whatsAppService.generateReferenceCode();
            if (!returnRequestRepository.existsByEvidenceReferenceCode(code)) {
                return code;
            }
        }
        throw new IllegalStateException("Failed to generate a unique evidence reference code after 5 attempts");
    }

    private Order resolveOwnedDeliveredOrder(Long orderId, CustomerProfile profile) {
        Order order = orderRepository.findByIdAndCustomerProfileId(orderId, profile.getId())
                .orElseThrow(() -> {
                    log.error("[2022] Order {} not found for customerProfileId={}", orderId, profile.getId());
                    return new NotFoundException("Order not found: " + orderId);
                });
        if (order.getStatus() != OrderStatus.DELIVERED) {
            log.error("[2023] Order {} is not eligible for return/exchange (status={})", orderId, order.getStatus());
            throw new ConflictException("This order is not eligible for return/exchange - it must be delivered first");
        }
        return order;
    }

    private OrderItem resolveOwnedItem(Order order, Long orderItemId) {
        return order.getItems().stream()
                .filter(i -> i.getId().equals(orderItemId))
                .findFirst()
                .orElseThrow(() -> {
                    log.error("[2024] Order item {} not found on order {}", orderItemId, order.getId());
                    return new NotFoundException("Order item not found: " + orderItemId);
                });
    }

    private ReturnRequest resolveOwnedRequest(Long requestId, CustomerProfile profile) {
        return returnRequestRepository.findByIdAndCustomerProfileId(requestId, profile.getId())
                .orElseThrow(() -> {
                    log.error("[2025] Return request {} not found for customerProfileId={}", requestId, profile.getId());
                    return new NotFoundException("Return request not found: " + requestId);
                });
    }

    private CustomerProfile resolveProfile(Long userId) {
        return customerProfileRepository.findByUserId(userId)
                .orElseThrow(() -> {
                    log.error("[2026] Customer profile not found for userId={}", userId);
                    return new NotFoundException("Customer profile not found for user: " + userId);
                });
    }

    private ReturnRequestSummaryResponse toSummaryResponse(ReturnRequest r) {
        return new ReturnRequestSummaryResponse(
                r.getId(), r.getOrder().getId(), r.getOrderItem().getProductName(), r.getRequestType(), r.getStatus(),
                r.getEvidenceStatus(), r.getCreatedAt());
    }

    private ReturnRequestDetailResponse toDetailResponse(ReturnRequest r) {
        String whatsappLink = r.getRequestType() == RequestType.DAMAGED_PRODUCT && r.getEvidenceReferenceCode() != null
                ? whatsAppService.buildEvidenceLink(r.getEvidenceReferenceCode())
                : null;
        return new ReturnRequestDetailResponse(
                r.getId(), r.getOrder().getId(), r.getOrderItem().getId(), r.getOrderItem().getProductName(),
                r.getRequestType(), r.getStatus(), r.getReason(), r.getRequestedSizeName(), r.getEvidenceStatus(),
                r.getEvidenceReferenceCode(), whatsappLink, r.getAdminNote(), r.getCreatedAt());
    }
}
