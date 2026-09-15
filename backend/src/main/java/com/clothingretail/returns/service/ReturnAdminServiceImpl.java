package com.clothingretail.returns.service;

import com.clothingretail.auth.User;
import com.clothingretail.auth.repository.UserRepository;
import com.clothingretail.common.AuditorNameResolver;
import com.clothingretail.common.ConflictException;
import com.clothingretail.common.NotFoundException;
import com.clothingretail.common.PageResponse;
import com.clothingretail.inventory.InventoryTransaction;
import com.clothingretail.inventory.InventoryTransactionType;
import com.clothingretail.inventory.repository.InventoryTransactionRepository;
import com.clothingretail.order.dto.AdminRefundRequest;
import com.clothingretail.order.dto.AdminRefundResponse;
import com.clothingretail.order.service.AdminOrderService;
import com.clothingretail.payment.Refund;
import com.clothingretail.payment.repository.RefundRepository;
import com.clothingretail.product.ProductVariant;
import com.clothingretail.product.repository.ProductVariantRepository;
import com.clothingretail.returns.EvidenceStatus;
import com.clothingretail.returns.RequestType;
import com.clothingretail.returns.ReturnRequest;
import com.clothingretail.returns.ReturnRequestStatus;
import com.clothingretail.returns.dto.AdminInitiateRefundRequest;
import com.clothingretail.returns.dto.AdminReturnDecisionRequest;
import com.clothingretail.returns.dto.AdminReturnDetailResponse;
import com.clothingretail.returns.dto.AdminReturnRow;
import com.clothingretail.returns.repository.ReturnRequestRepository;
import com.clothingretail.returns.repository.ReturnRequestSpecifications;
import java.time.Instant;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@Log4j2
public class ReturnAdminServiceImpl implements ReturnAdminService {

    private final ReturnRequestRepository returnRequestRepository;
    private final ProductVariantRepository productVariantRepository;
    private final InventoryTransactionRepository inventoryTransactionRepository;
    private final ReturnEvidenceStorageService evidenceStorageService;
    private final AdminOrderService adminOrderService;
    private final RefundRepository refundRepository;
    private final UserRepository userRepository;
    private final AuditorNameResolver auditorNameResolver;

    public ReturnAdminServiceImpl(
            ReturnRequestRepository returnRequestRepository,
            ProductVariantRepository productVariantRepository,
            InventoryTransactionRepository inventoryTransactionRepository,
            ReturnEvidenceStorageService evidenceStorageService,
            AdminOrderService adminOrderService,
            RefundRepository refundRepository,
            UserRepository userRepository,
            AuditorNameResolver auditorNameResolver) {
        this.returnRequestRepository = returnRequestRepository;
        this.productVariantRepository = productVariantRepository;
        this.inventoryTransactionRepository = inventoryTransactionRepository;
        this.evidenceStorageService = evidenceStorageService;
        this.adminOrderService = adminOrderService;
        this.refundRepository = refundRepository;
        this.userRepository = userRepository;
        this.auditorNameResolver = auditorNameResolver;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AdminReturnRow> list(RequestType requestType, ReturnRequestStatus status, String q, int page, int size) {
        log.info("[2038] Listing admin return requests: requestType={}, status={}, q={}, page={}, size={}", requestType, status, q, page, size);
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Specification<ReturnRequest> spec = ReturnRequestSpecifications.filter(requestType, status, q);
        Page<ReturnRequest> results = returnRequestRepository.findAll(spec, pageable);
        log.info("[2039] Admin return request list result: {} of {} total matched", results.getNumberOfElements(), results.getTotalElements());
        return PageResponse.of(results, results.getContent().stream().map(this::toRow).toList());
    }

    @Override
    @Transactional(readOnly = true)
    public AdminReturnDetailResponse detail(Long id) {
        return toAdminDetailResponse(findRequest(id));
    }

    @Override
    @Transactional
    public AdminReturnDetailResponse approveExchange(Long id, AdminReturnDecisionRequest request, Long adminUserId) {
        log.info("[2040] approveExchange requested: id={}, adminUserId={}", id, adminUserId);
        ReturnRequest rr = findRequestForUpdate(id);
        if (rr.getRequestType() != RequestType.SIZE_EXCHANGE) {
            log.error("[2041] Approve-exchange rejected: request {} is type {} (not SIZE_EXCHANGE)", id, rr.getRequestType());
            throw new ConflictException("This request is not a size-exchange request");
        }
        requirePending(rr);

        ProductVariant originalVariant = rr.getOrderItem().getProductVariant();
        if (originalVariant == null) {
            log.error("[2042] Approve-exchange rejected: original variant no longer exists, requestId={}", id);
            throw new ConflictException("The original product variant no longer exists");
        }
        ProductVariant requestedVariant = rr.getRequestedVariant();
        if (requestedVariant == null) {
            log.error("[2043] Approve-exchange rejected: requested variant no longer exists, requestId={}", id);
            throw new ConflictException("The requested replacement variant no longer exists");
        }
        int quantity = rr.getOrderItem().getQuantity();
        User actingAdmin = adminUserId != null ? userRepository.findById(adminUserId).orElse(null) : null;

        // Re-validates availability at approval time (stock may have moved since the request) -
        // nothing else is mutated yet if this fails, so the whole method rolls back cleanly.
        int previousReplacementStock = productVariantRepository.getStockQuantity(requestedVariant.getId());
        int decremented = productVariantRepository.decrementStockForExchange(requestedVariant.getId(), quantity);
        if (decremented == 0) {
            log.error("[2044] Approve-exchange rejected: replacement variant {} no longer has {} unit(s) in stock", requestedVariant.getId(), quantity);
            throw new ConflictException("The requested size is no longer in stock");
        }
        int newReplacementStock = productVariantRepository.getStockQuantity(requestedVariant.getId());

        int previousOriginalStock = productVariantRepository.getStockQuantity(originalVariant.getId());
        productVariantRepository.restockOnReturn(originalVariant.getId(), quantity);
        int newOriginalStock = productVariantRepository.getStockQuantity(originalVariant.getId());

        writeInventoryTransaction(originalVariant, InventoryTransactionType.RETURN_IN, quantity,
                previousOriginalStock, newOriginalStock, rr.getId(), actingAdmin);
        writeInventoryTransaction(requestedVariant, InventoryTransactionType.SALE_OUT, quantity,
                previousReplacementStock, newReplacementStock, rr.getId(), actingAdmin);

        markReviewed(rr, ReturnRequestStatus.APPROVED, request.note(), adminUserId);
        log.info("[2045] Exchange approved: id={}, originalVariant={} ({}->{}), replacementVariant={} ({}->{})",
                rr.getId(), originalVariant.getId(), previousOriginalStock, newOriginalStock,
                requestedVariant.getId(), previousReplacementStock, newReplacementStock);

        return toAdminDetailResponse(rr);
    }

    @Override
    @Transactional
    public AdminReturnDetailResponse approveDamageClaim(Long id, AdminReturnDecisionRequest request, Long adminUserId) {
        log.info("[2046] approveDamageClaim requested: id={}, adminUserId={}", id, adminUserId);
        ReturnRequest rr = findRequestForUpdate(id);
        if (rr.getRequestType() != RequestType.DAMAGED_PRODUCT) {
            log.error("[2047] Approve-damage rejected: request {} is type {} (not DAMAGED_PRODUCT)", id, rr.getRequestType());
            throw new ConflictException("This request is not a damaged-product request");
        }
        requirePending(rr);
        if (rr.getEvidenceStatus() != EvidenceStatus.SUBMITTED) {
            log.error("[2048] Approve-damage rejected: request {} has no submitted video evidence", id);
            throw new ConflictException("Video evidence has not been submitted for this request yet");
        }

        markReviewed(rr, ReturnRequestStatus.APPROVED, request.note(), adminUserId);
        log.info("[2049] Damage claim approved: id={}", rr.getId());
        return toAdminDetailResponse(rr);
    }

    @Override
    @Transactional
    public AdminReturnDetailResponse reject(Long id, AdminReturnDecisionRequest request, Long adminUserId) {
        log.info("[2050] reject requested: id={}, adminUserId={}", id, adminUserId);
        ReturnRequest rr = findRequestForUpdate(id);
        requirePending(rr);

        markReviewed(rr, ReturnRequestStatus.REJECTED, request.note(), adminUserId);
        log.info("[2051] Return request rejected: id={}", rr.getId());
        return toAdminDetailResponse(rr);
    }

    @Override
    @Transactional
    public AdminReturnDetailResponse uploadEvidence(Long id, MultipartFile file) {
        log.info("[2052] uploadEvidence requested: id={}", id);
        ReturnRequest rr = findRequestForUpdate(id);
        if (rr.getRequestType() != RequestType.DAMAGED_PRODUCT) {
            log.error("[2053] Evidence upload rejected: request {} is type {} (not DAMAGED_PRODUCT)", id, rr.getRequestType());
            throw new ConflictException("Evidence can only be attached to damaged-product requests");
        }
        if (rr.getStatus() == ReturnRequestStatus.REJECTED || rr.getStatus() == ReturnRequestStatus.REFUNDED) {
            log.error("[2054] Evidence upload rejected: request {} is already finalized (status={})", id, rr.getStatus());
            throw new ConflictException("This request has already been finalized (status: " + rr.getStatus() + ")");
        }

        ReturnEvidenceStorageService.StoredEvidence stored = evidenceStorageService.store(file);
        rr.setEvidenceFilename(stored.filename());
        rr.setEvidenceContentType(stored.contentType());
        rr.setEvidenceUploadedAt(Instant.now());
        rr.setEvidenceStatus(EvidenceStatus.SUBMITTED);
        returnRequestRepository.save(rr);
        log.info("[2055] Evidence attached to return request {}: filename={}", id, stored.filename());

        return toAdminDetailResponse(rr);
    }

    @Override
    @Transactional
    public AdminReturnDetailResponse initiateRefund(Long id, AdminInitiateRefundRequest request, Long adminUserId) {
        log.info("[2056] initiateRefund requested: id={}, adminUserId={}, amount={}", id, adminUserId, request.amount());
        ReturnRequest rr = findRequestForUpdate(id);

        if (rr.getRequestType() != RequestType.DAMAGED_PRODUCT) {
            log.error("[2057] Refund rejected: request {} is not a damaged-product request (type={})", id, rr.getRequestType());
            throw new ConflictException("Refund is only available for damaged-product requests");
        }
        if (rr.getEvidenceStatus() != EvidenceStatus.SUBMITTED) {
            log.error("[2058] Refund rejected: request {} has no submitted video evidence", id);
            throw new ConflictException("Video evidence has not been submitted for this request");
        }
        // status != APPROVED also structurally blocks a second refund attempt, since the first
        // successful call already flips status to REFUNDED before releasing the row lock this
        // method holds - see ReturnRequestRepository.findByIdForUpdate's own doc comment.
        if (rr.getStatus() != ReturnRequestStatus.APPROVED) {
            log.error("[2059] Refund rejected: request {} status is {} (not APPROVED)", id, rr.getStatus());
            throw new ConflictException("The damage claim has not been approved");
        }

        AdminRefundRequest delegated = new AdminRefundRequest(request.amount(), request.reason());
        AdminRefundResponse refundResult = adminOrderService.refund(rr.getOrder().getId(), delegated, adminUserId);

        Refund refund = refundRepository.findById(refundResult.id())
                .orElseThrow(() -> new IllegalStateException(
                        "Refund " + refundResult.id() + " was just created but could not be reloaded"));
        rr.setRefund(refund);
        rr.setStatus(ReturnRequestStatus.REFUNDED);
        returnRequestRepository.save(rr);
        log.info("[2060] Refund initiated for return request {}: refundId={}, amount={}", id, refund.getId(), refund.getAmount());

        return toAdminDetailResponse(rr);
    }

    @Override
    @Transactional(readOnly = true)
    public EvidenceFile loadEvidence(Long id) {
        ReturnRequest rr = findRequest(id);
        if (rr.getEvidenceFilename() == null) {
            log.error("[2061] Evidence fetch rejected: no evidence uploaded yet for request {}", id);
            throw new NotFoundException("No evidence has been uploaded for this request yet");
        }
        return new EvidenceFile(evidenceStorageService.load(rr.getEvidenceFilename()), rr.getEvidenceContentType());
    }

    private void requirePending(ReturnRequest rr) {
        if (rr.getStatus() != ReturnRequestStatus.PENDING) {
            log.error("[2062] Rejected: request {} has already been reviewed (status={})", rr.getId(), rr.getStatus());
            throw new ConflictException("This request has already been reviewed (status: " + rr.getStatus() + ")");
        }
    }

    private void markReviewed(ReturnRequest rr, ReturnRequestStatus status, String note, Long adminUserId) {
        rr.setStatus(status);
        rr.setAdminNote(note);
        rr.setReviewedBy(adminUserId);
        rr.setReviewedAt(Instant.now());
        returnRequestRepository.save(rr);
    }

    private void writeInventoryTransaction(
            ProductVariant variant, InventoryTransactionType type, int quantity, int previousQuantity, int newQuantity,
            Long returnRequestId, User actingAdmin) {
        InventoryTransaction transaction = new InventoryTransaction();
        transaction.setProductVariant(variant);
        transaction.setType(type);
        transaction.setQuantity(quantity);
        transaction.setPreviousQuantity(previousQuantity);
        transaction.setNewQuantity(newQuantity);
        transaction.setReferenceType("RETURN_REQUEST");
        transaction.setReferenceId(returnRequestId);
        transaction.setReason("Size exchange approved - Return request " + returnRequestId);
        transaction.setPerformedBy(actingAdmin);
        inventoryTransactionRepository.save(transaction);
    }

    private ReturnRequest findRequest(Long id) {
        return returnRequestRepository.findById(id).orElseThrow(() -> {
            log.error("[2063] Return request not found: id={}", id);
            return new NotFoundException("Return request not found: " + id);
        });
    }

    private ReturnRequest findRequestForUpdate(Long id) {
        return returnRequestRepository.findByIdForUpdate(id).orElseThrow(() -> {
            log.error("[2064] Return request not found: id={}", id);
            return new NotFoundException("Return request not found: " + id);
        });
    }

    private AdminReturnRow toRow(ReturnRequest r) {
        return new AdminReturnRow(
                r.getId(),
                r.getOrder().getId(),
                r.getOrder().getOrderNumber(),
                r.getCustomerProfile().getUser().getFullName(),
                r.getOrderItem().getProductName(),
                r.getOrderItem().getSku(),
                r.getOrderItem().getSizeName(),
                r.getRequestedSizeName(),
                r.getRequestType(),
                r.getReason(),
                r.getStatus(),
                r.getEvidenceStatus(),
                r.getCreatedAt());
    }

    private AdminReturnDetailResponse toAdminDetailResponse(ReturnRequest r) {
        AdminRefundResponse refundResponse = r.getRefund() != null
                ? new AdminRefundResponse(r.getRefund().getId(), r.getRefund().getAmount(), r.getRefund().getStatus(),
                        r.getRefund().getReference(), r.getRefund().getCreatedAt())
                : null;
        return new AdminReturnDetailResponse(
                r.getId(),
                r.getOrder().getId(),
                r.getOrder().getOrderNumber(),
                r.getCustomerProfile().getUser().getFullName(),
                r.getCustomerProfile().getUser().getEmail(),
                r.getCustomerProfile().getUser().getMobileNumber(),
                r.getOrderItem().getProductName(),
                r.getOrderItem().getSku(),
                r.getOrderItem().getSizeName(),
                r.getRequestedSizeName(),
                r.getRequestType(),
                r.getReason(),
                r.getStatus(),
                r.getEvidenceStatus(),
                r.getEvidenceFilename() != null,
                r.getEvidenceReferenceCode(),
                r.getAdminNote(),
                auditorNameResolver.resolve(r.getReviewedBy()),
                r.getReviewedAt(),
                refundResponse,
                r.getCreatedAt());
    }
}
