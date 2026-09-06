package com.clothingretail.inventory;

import com.clothingretail.auth.User;
import com.clothingretail.auth.UserRepository;
import com.clothingretail.common.BadRequestException;
import com.clothingretail.common.NotFoundException;
import com.clothingretail.inventory.dto.DamageRequest;
import com.clothingretail.inventory.dto.DamageResponse;
import com.clothingretail.inventory.dto.StockAdjustRequest;
import com.clothingretail.inventory.dto.StockAdjustResponse;
import com.clothingretail.product.ProductVariant;
import com.clothingretail.product.ProductVariantRepository;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The only place {@code ProductVariant.stockQuantity}/{@code damagedQuantity} change for an
 * already-existing variant (besides {@link PurchaseService}, which is the one place stock
 * increases via a purchase). Every mutation here also appends an immutable
 * {@link InventoryTransaction} row in the same transaction, so the audit log and the live
 * counters can never drift apart.
 */
@Service
@Log4j2
public class StockService {

    private final ProductVariantRepository productVariantRepository;
    private final InventoryTransactionRepository inventoryTransactionRepository;
    private final DamageRecordRepository damageRecordRepository;
    private final DamageReasonRepository damageReasonRepository;
    private final UserRepository userRepository;

    public StockService(
            ProductVariantRepository productVariantRepository,
            InventoryTransactionRepository inventoryTransactionRepository,
            DamageRecordRepository damageRecordRepository,
            DamageReasonRepository damageReasonRepository,
            UserRepository userRepository) {
        this.productVariantRepository = productVariantRepository;
        this.inventoryTransactionRepository = inventoryTransactionRepository;
        this.damageRecordRepository = damageRecordRepository;
        this.damageReasonRepository = damageReasonRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public StockAdjustResponse adjust(Long variantId, StockAdjustRequest request, Long actingUserId) {
        log.info(
                "[1335] Adjusting stock variantId={}, quantityChange={}, reason={}, actingUserId={}",
                variantId, request.quantityChange(), request.reason(), actingUserId);
        ProductVariant variant = productVariantRepository.findById(variantId)
                .orElseThrow(() -> {
                    log.error("[1336] Stock adjustment failed - variant not found id={}", variantId);
                    return new NotFoundException("Product variant not found: " + variantId);
                });

        int previousQuantity = variant.getStockQuantity();
        int newQuantity = previousQuantity + request.quantityChange();
        log.info("[1337] Computed stock adjustment variantId={}, previousQuantity={}, newQuantity={}", variantId, previousQuantity, newQuantity);
        if (newQuantity < 0) {
            log.error(
                    "[1338] Stock adjustment rejected - would go negative variantId={}, previousQuantity={}, quantityChange={}",
                    variantId, previousQuantity, request.quantityChange());
            throw new BadRequestException(
                    "Adjustment would reduce stock below zero (current: " + previousQuantity + ", change: " + request.quantityChange() + ")");
        }

        variant.setStockQuantity(newQuantity);
        productVariantRepository.save(variant);

        InventoryTransaction transaction = new InventoryTransaction();
        transaction.setProductVariant(variant);
        transaction.setType(InventoryTransactionType.ADJUSTMENT);
        transaction.setQuantity(Math.abs(request.quantityChange()));
        transaction.setPreviousQuantity(previousQuantity);
        transaction.setNewQuantity(newQuantity);
        transaction.setReason(request.reason());
        transaction.setPerformedBy(resolveUser(actingUserId));
        inventoryTransactionRepository.save(transaction);

        log.info(
                "[1339] Stock adjusted variantId={}, previousQuantity={}, newQuantity={}, availableQuantity={}",
                variant.getId(), previousQuantity, newQuantity, variant.getAvailableQuantity());
        return new StockAdjustResponse(variant.getId(), previousQuantity, newQuantity, variant.getAvailableQuantity());
    }

    @Transactional
    public DamageResponse recordDamage(Long variantId, DamageRequest request, Long actingUserId) {
        log.info(
                "[1340] Recording damage variantId={}, reasonId={}, quantity={}, actingUserId={}",
                variantId, request.reasonId(), request.quantity(), actingUserId);
        ProductVariant variant = productVariantRepository.findById(variantId)
                .orElseThrow(() -> {
                    log.error("[1341] Damage record failed - variant not found id={}", variantId);
                    return new NotFoundException("Product variant not found: " + variantId);
                });

        DamageReason reason = damageReasonRepository.findById(request.reasonId())
                .filter(DamageReason::isActive)
                .orElseThrow(() -> {
                    log.error("[1342] Damage record failed - damage reason not found or inactive id={}", request.reasonId());
                    return new NotFoundException("Damage reason not found: " + request.reasonId());
                });

        int quantity = request.quantity();
        log.info(
                "[1343] Damage quantity check variantId={}, requestedQuantity={}, availableQuantity={}",
                variantId, quantity, variant.getAvailableQuantity());
        if (quantity > variant.getAvailableQuantity()) {
            log.error(
                    "[1344] Damage rejected - quantity {} exceeds available {} for variantId={}",
                    quantity, variant.getAvailableQuantity(), variantId);
            throw new BadRequestException(
                    "Cannot damage " + quantity + " units - only " + variant.getAvailableQuantity() + " currently available");
        }

        int previousDamaged = variant.getDamagedQuantity();
        int newDamaged = previousDamaged + quantity;
        log.info("[1345] Damaged quantity update variantId={}, previousDamaged={}, newDamaged={}", variantId, previousDamaged, newDamaged);
        variant.setDamagedQuantity(newDamaged);
        productVariantRepository.save(variant);

        User reportedBy = resolveUser(actingUserId);

        DamageRecord record = new DamageRecord();
        record.setProductVariant(variant);
        record.setQuantity(quantity);
        record.setReason(reason);
        record.setNotes(request.notes());
        record.setReportedBy(reportedBy);
        damageRecordRepository.save(record);

        InventoryTransaction transaction = new InventoryTransaction();
        transaction.setProductVariant(variant);
        transaction.setType(InventoryTransactionType.DAMAGE);
        transaction.setQuantity(quantity);
        transaction.setPreviousQuantity(previousDamaged);
        transaction.setNewQuantity(newDamaged);
        transaction.setReason(reason.getName());
        transaction.setPerformedBy(reportedBy);
        transaction.setReferenceType("DAMAGE_RECORD");
        inventoryTransactionRepository.save(transaction);
        // referenceId needs the persisted DamageRecord's id, which IDENTITY generation
        // has already assigned by the time save() above returns.
        transaction.setReferenceId(record.getId());
        inventoryTransactionRepository.save(transaction);

        log.info(
                "[1346] Damage recorded variantId={}, damageRecordId={}, newDamaged={}, availableQuantity={}",
                variant.getId(), record.getId(), newDamaged, variant.getAvailableQuantity());
        return new DamageResponse(variant.getId(), newDamaged, variant.getAvailableQuantity());
    }

    /**
     * Writes the one-off ADJUSTMENT transaction for a brand-new variant's initial stock
     * (set via the product form on create - see {@code ProductService.applyVariants}).
     * Does NOT touch stockQuantity itself - the caller already set it before saving the
     * variant; this only records the audit trail entry.
     */
    @Transactional
    public void recordInitialStock(ProductVariant variant, int initialQuantity, Long actingUserId) {
        log.info(
                "[1347] Recording initial stock variantId={}, initialQuantity={}, actingUserId={}",
                variant.getId(), initialQuantity, actingUserId);
        if (initialQuantity <= 0) {
            log.info("[1348] Skipping initial stock record - non-positive quantity variantId={}, initialQuantity={}", variant.getId(), initialQuantity);
            return;
        }
        InventoryTransaction transaction = new InventoryTransaction();
        transaction.setProductVariant(variant);
        transaction.setType(InventoryTransactionType.ADJUSTMENT);
        transaction.setQuantity(initialQuantity);
        transaction.setPreviousQuantity(0);
        transaction.setNewQuantity(initialQuantity);
        transaction.setReason("Initial stock on creation");
        transaction.setPerformedBy(resolveUser(actingUserId));
        inventoryTransactionRepository.save(transaction);
        log.info("[1349] Initial stock transaction recorded variantId={}, initialQuantity={}", variant.getId(), initialQuantity);
    }

    private User resolveUser(Long userId) {
        return userId != null ? userRepository.findById(userId).orElse(null) : null;
    }
}
