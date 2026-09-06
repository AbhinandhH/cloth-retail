package com.clothingretail.inventory;

import com.clothingretail.inventory.dto.DamageRecordRow;
import com.clothingretail.inventory.dto.DashboardResponse;
import com.clothingretail.inventory.dto.InventoryTransactionRow;
import com.clothingretail.inventory.dto.RecentProductRow;
import com.clothingretail.inventory.dto.VariantInventoryRow;
import com.clothingretail.product.Product;
import com.clothingretail.product.ProductRepository;
import com.clothingretail.product.ProductStatus;
import com.clothingretail.product.ProductVariant;
import com.clothingretail.product.ProductVariantRepository;
import java.time.Instant;
import java.util.List;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Read-side of the admin inventory module: the variant-level listing with filters/sort,
 * transaction and damage history listings, and the dashboard aggregate. Kept separate from
 * {@link StockService} (the write side) to keep each class focused.
 */
@Service
@Transactional(readOnly = true)
@Log4j2
public class InventoryQueryService {

    private static final int DASHBOARD_RECENT_LIMIT_10 = 10;
    private static final int DASHBOARD_RECENT_LIMIT_5 = 5;

    private final ProductVariantRepository productVariantRepository;
    private final ProductRepository productRepository;
    private final InventoryTransactionRepository inventoryTransactionRepository;
    private final DamageRecordRepository damageRecordRepository;

    public InventoryQueryService(
            ProductVariantRepository productVariantRepository,
            ProductRepository productRepository,
            InventoryTransactionRepository inventoryTransactionRepository,
            DamageRecordRepository damageRecordRepository) {
        this.productVariantRepository = productVariantRepository;
        this.productRepository = productRepository;
        this.inventoryTransactionRepository = inventoryTransactionRepository;
        this.damageRecordRepository = damageRecordRepository;
    }

    public Page<VariantInventoryRow> listVariants(
            String q,
            Long categoryId,
            Long colorId,
            Long sizeId,
            StockStatus stockStatus,
            ProductStatus productStatus,
            String sort,
            String dir,
            int page,
            int size) {
        log.info(
                "[1314] Listing variants q={}, categoryId={}, colorId={}, sizeId={}, stockStatus={}, productStatus={}, sort={}, dir={}, page={}, size={}",
                q, categoryId, colorId, sizeId, stockStatus, productStatus, sort, dir, page, size);
        Pageable pageable = PageRequest.of(page, size, variantSort(sort, dir));
        var spec = InventorySpecifications.filterVariants(q, categoryId, colorId, sizeId, stockStatus, productStatus);
        Page<VariantInventoryRow> result = productVariantRepository.findAll(spec, pageable).map(this::toRow);
        log.info("[1315] Variant listing returned {} of {} total element(s)", result.getNumberOfElements(), result.getTotalElements());
        return result;
    }

    public Page<InventoryTransactionRow> listTransactions(
            Long variantId, InventoryTransactionType type, Instant dateFrom, Instant dateTo, int page, int size) {
        log.info(
                "[1316] Listing inventory transactions variantId={}, type={}, dateFrom={}, dateTo={}, page={}, size={}",
                variantId, type, dateFrom, dateTo, page, size);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        var spec = InventorySpecifications.filterTransactions(variantId, type, dateFrom, dateTo);
        Page<InventoryTransactionRow> result = inventoryTransactionRepository.findAll(spec, pageable).map(this::toRow);
        log.info("[1317] Transaction listing returned {} of {} total element(s)", result.getNumberOfElements(), result.getTotalElements());
        return result;
    }

    public Page<InventoryTransactionRow> listVariantTransactions(Long variantId, int page, int size) {
        log.info("[1318] Listing transactions for variantId={}, page={}, size={}", variantId, page, size);
        return listTransactions(variantId, null, null, null, page, size);
    }

    public Page<DamageRecordRow> listDamages(Long variantId, int page, int size) {
        log.info("[1319] Listing damage records variantId={}, page={}, size={}", variantId, page, size);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        var spec = InventorySpecifications.filterDamages(variantId);
        Page<DamageRecordRow> result = damageRecordRepository.findAll(spec, pageable).map(this::toRow);
        log.info("[1320] Damage record listing returned {} of {} total element(s)", result.getNumberOfElements(), result.getTotalElements());
        return result;
    }

    public DashboardResponse dashboard() {
        log.info("[1321] Building inventory dashboard");
        long totalProducts = productRepository.count();
        long totalVariants = productVariantRepository.count();
        long totalAvailableStock = productVariantRepository.sumAvailableQuantity();
        long totalDamagedStock = productVariantRepository.sumDamagedQuantity();
        log.info(
                "[1322] Dashboard totals: products={}, variants={}, availableStock={}, damagedStock={}",
                totalProducts, totalVariants, totalAvailableStock, totalDamagedStock);

        var lowStockSpec = InventorySpecifications.filterVariants(null, null, null, null, StockStatus.LOW_STOCK, null);
        var outOfStockSpec = InventorySpecifications.filterVariants(null, null, null, null, StockStatus.OUT_OF_STOCK, null);
        long lowStockCount = productVariantRepository.count(lowStockSpec);
        long outOfStockCount = productVariantRepository.count(outOfStockSpec);
        log.info("[1323] Dashboard stock alerts: lowStockCount={}, outOfStockCount={}", lowStockCount, outOfStockCount);

        Sort byStockAsc = Sort.by(Sort.Direction.ASC, "stockQuantity");
        List<VariantInventoryRow> lowStockItems = productVariantRepository
                .findAll(lowStockSpec, PageRequest.of(0, DASHBOARD_RECENT_LIMIT_10, byStockAsc))
                .map(this::toRow)
                .getContent();
        List<VariantInventoryRow> outOfStockItems = productVariantRepository
                .findAll(outOfStockSpec, PageRequest.of(0, DASHBOARD_RECENT_LIMIT_10, byStockAsc))
                .map(this::toRow)
                .getContent();

        List<RecentProductRow> recentProducts = productRepository
                .findAll(PageRequest.of(0, DASHBOARD_RECENT_LIMIT_5, Sort.by(Sort.Direction.DESC, "createdAt")))
                .map(p -> new RecentProductRow(p.getId(), p.getName(), p.getSlug(), p.getStatus(), p.getCreatedAt()))
                .getContent();

        List<InventoryTransactionRow> recentTransactions = inventoryTransactionRepository
                .findAll(PageRequest.of(0, DASHBOARD_RECENT_LIMIT_10, Sort.by(Sort.Direction.DESC, "createdAt")))
                .map(this::toRow)
                .getContent();

        List<DamageRecordRow> recentDamages = damageRecordRepository
                .findAll(PageRequest.of(0, DASHBOARD_RECENT_LIMIT_10, Sort.by(Sort.Direction.DESC, "createdAt")))
                .map(this::toRow)
                .getContent();

        log.info(
                "[1324] Dashboard built: recentProducts={}, recentTransactions={}, recentDamages={}",
                recentProducts.size(), recentTransactions.size(), recentDamages.size());
        return new DashboardResponse(
                totalProducts,
                totalVariants,
                totalAvailableStock,
                lowStockCount,
                outOfStockCount,
                totalDamagedStock,
                lowStockItems,
                outOfStockItems,
                recentProducts,
                recentTransactions,
                recentDamages);
    }

    private Sort variantSort(String sort, String dir) {
        Sort.Direction direction = "desc".equalsIgnoreCase(dir) ? Sort.Direction.DESC : Sort.Direction.ASC;
        String property =
                switch (sort == null ? "" : sort) {
                    case "name" -> "product.name";
                    case "stock" -> "stockQuantity";
                    case "updatedAt" -> "updatedAt";
                    default -> "updatedAt";
                };
        return Sort.by(direction, property);
    }

    private VariantInventoryRow toRow(ProductVariant v) {
        Product product = v.getProduct();
        return new VariantInventoryRow(
                v.getId(),
                product.getId(),
                product.getName(),
                product.getSlug(),
                v.getSku(),
                product.getCategory().getName(),
                v.getColor().getName(),
                v.getColor().getHexCode(),
                v.getSize().getName(),
                v.getSellingPrice(),
                v.getCostPrice(),
                v.getStockQuantity(),
                v.getReservedQuantity(),
                v.getDamagedQuantity(),
                v.getAvailableQuantity(),
                v.getLowStockThreshold(),
                InventorySpecifications.computeStockStatus(v),
                v.isActive(),
                product.getStatus(),
                v.getUpdatedAt());
    }

    private InventoryTransactionRow toRow(InventoryTransaction t) {
        ProductVariant v = t.getProductVariant();
        return new InventoryTransactionRow(
                t.getId(),
                v.getId(),
                v.getSku(),
                v.getProduct().getName(),
                t.getType(),
                t.getQuantity(),
                t.getPreviousQuantity(),
                t.getNewQuantity(),
                t.getReason(),
                t.getReferenceType(),
                t.getReferenceId(),
                t.getPerformedBy() != null ? t.getPerformedBy().getFullName() : null,
                t.getCreatedAt());
    }

    private DamageRecordRow toRow(DamageRecord d) {
        ProductVariant v = d.getProductVariant();
        return new DamageRecordRow(
                d.getId(),
                v.getId(),
                v.getSku(),
                v.getProduct().getName(),
                d.getQuantity(),
                d.getReason().getId(),
                d.getReason().getName(),
                d.getNotes(),
                d.getReportedBy() != null ? d.getReportedBy().getFullName() : null,
                d.getCreatedAt());
    }
}
