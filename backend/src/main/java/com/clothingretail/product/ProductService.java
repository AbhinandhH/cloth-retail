package com.clothingretail.product;

import com.clothingretail.common.ConflictException;
import com.clothingretail.common.NotFoundException;
import com.clothingretail.inventory.DamageRecordRepository;
import com.clothingretail.inventory.InventoryTransactionRepository;
import com.clothingretail.inventory.PurchaseItemRepository;
import com.clothingretail.inventory.StockService;
import com.clothingretail.masterdata.Brand;
import com.clothingretail.masterdata.BrandRepository;
import com.clothingretail.masterdata.Category;
import com.clothingretail.masterdata.CategoryRepository;
import com.clothingretail.masterdata.Color;
import com.clothingretail.masterdata.ColorRepository;
import com.clothingretail.masterdata.Material;
import com.clothingretail.masterdata.MaterialRepository;
import com.clothingretail.masterdata.Size;
import com.clothingretail.masterdata.SizeRepository;
import com.clothingretail.masterdata.SubCategory;
import com.clothingretail.masterdata.SubCategoryRepository;
import com.clothingretail.product.dto.ProductAdminRequest;
import com.clothingretail.product.dto.ProductAdminResponse;
import com.clothingretail.product.dto.ProductAdminSummaryResponse;
import com.clothingretail.product.dto.ProductDetailResponse;
import com.clothingretail.product.dto.ProductSummaryResponse;
import com.clothingretail.product.dto.VariantAdminRequest;
import com.clothingretail.product.dto.VariantAdminResponse;
import com.clothingretail.product.dto.VariantImageRequest;
import com.clothingretail.product.dto.VariantImageResponse;
import com.clothingretail.product.dto.VariantResponse;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@Log4j2
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final CategoryRepository categoryRepository;
    private final SubCategoryRepository subCategoryRepository;
    private final BrandRepository brandRepository;
    private final MaterialRepository materialRepository;
    private final SizeRepository sizeRepository;
    private final ColorRepository colorRepository;
    private final InventoryTransactionRepository inventoryTransactionRepository;
    private final PurchaseItemRepository purchaseItemRepository;
    private final DamageRecordRepository damageRecordRepository;
    private final StockService stockService;

    public ProductService(
            ProductRepository productRepository,
            ProductVariantRepository productVariantRepository,
            CategoryRepository categoryRepository,
            SubCategoryRepository subCategoryRepository,
            BrandRepository brandRepository,
            MaterialRepository materialRepository,
            SizeRepository sizeRepository,
            ColorRepository colorRepository,
            InventoryTransactionRepository inventoryTransactionRepository,
            PurchaseItemRepository purchaseItemRepository,
            DamageRecordRepository damageRecordRepository,
            StockService stockService) {
        this.productRepository = productRepository;
        this.productVariantRepository = productVariantRepository;
        this.categoryRepository = categoryRepository;
        this.subCategoryRepository = subCategoryRepository;
        this.brandRepository = brandRepository;
        this.materialRepository = materialRepository;
        this.sizeRepository = sizeRepository;
        this.colorRepository = colorRepository;
        this.inventoryTransactionRepository = inventoryTransactionRepository;
        this.purchaseItemRepository = purchaseItemRepository;
        this.damageRecordRepository = damageRecordRepository;
        this.stockService = stockService;
    }

    public Page<ProductSummaryResponse> list(
            Long categoryId,
            Long subCategoryId,
            Long sizeId,
            Long colorId,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            String q,
            Pageable pageable) {
        log.info("[1900] List products categoryId={} subCategoryId={} sizeId={} colorId={} minPrice={} maxPrice={} q={}",
                categoryId, subCategoryId, sizeId, colorId, minPrice, maxPrice, q);
        var spec = ProductSpecifications.filter(categoryId, subCategoryId, sizeId, colorId, minPrice, maxPrice, q, true);
        Page<Product> page = productRepository.findAll(spec, pageable);
        Page<ProductSummaryResponse> result = page.map(this::toSummary);
        log.info("[1901] Products listed totalElements={}", result.getTotalElements());
        return result;
    }

    public ProductDetailResponse getBySlug(String slug) {
        log.info("[1902] Fetch product by slug={}", slug);
        Product product = productRepository.findBySlugAndStatus(slug, ProductStatus.ACTIVE)
                .orElseThrow(() -> {
                    log.error("[1903] Product not found slug={}", slug);
                    return new NotFoundException("Product not found: " + slug);
                });
        log.info("[1904] Product resolved productId={} slug={}", product.getId(), slug);
        return toDetail(product);
    }

    public Page<ProductAdminSummaryResponse> listAdmin(String q, Long categoryId, ProductStatus status, Pageable pageable) {
        log.info("[1905] List admin products q={} categoryId={} status={}", q, categoryId, status);
        var spec = ProductSpecifications.adminFilter(q, categoryId, status);
        Page<ProductAdminSummaryResponse> result = productRepository.findAll(spec, pageable).map(this::toAdminSummary);
        log.info("[1906] Admin products listed totalElements={}", result.getTotalElements());
        return result;
    }

    public ProductAdminResponse getAdmin(Long id) {
        log.info("[1907] Fetch admin product id={}", id);
        ProductAdminResponse response = toAdminResponse(findProduct(id));
        log.info("[1908] Admin product resolved productId={}", id);
        return response;
    }

    @Transactional
    public ProductAdminResponse create(ProductAdminRequest request, Long actingUserId) {
        log.info("[1909] Create product slug={} actingUserId={}", request.slug(), actingUserId);
        if (productRepository.existsBySlugIgnoreCase(request.slug())) {
            log.error("[1910] Create product failed, slug already exists slug={}", request.slug());
            throw new ConflictException("A product with this slug already exists");
        }
        Product product = new Product();
        applyProductFields(product, request);
        List<NewVariantStock> newVariantStocks = applyVariants(product, request.variants());
        Product saved = productRepository.save(product);
        log.info("[1911] Product created productId={} slug={}", saved.getId(), saved.getSlug());
        recordInitialStockTransactions(newVariantStocks, actingUserId);
        if (!newVariantStocks.isEmpty()) {
            log.info("[1912] Initial stock transactions recorded productId={} newVariantCount={}", saved.getId(), newVariantStocks.size());
        }
        ProductAdminResponse response = toAdminResponse(saved);
        log.info("[1913] Create product completed productId={}", saved.getId());
        return response;
    }

    @Transactional
    public ProductAdminResponse update(Long id, ProductAdminRequest request, Long actingUserId) {
        log.info("[1914] Update product id={} actingUserId={}", id, actingUserId);
        Product product = findProduct(id);
        applyProductFields(product, request);
        List<NewVariantStock> newVariantStocks = applyVariants(product, request.variants());
        Product saved = productRepository.save(product);
        log.info("[1915] Product updated productId={}", saved.getId());
        recordInitialStockTransactions(newVariantStocks, actingUserId);
        if (!newVariantStocks.isEmpty()) {
            log.info("[1916] Initial stock transactions recorded productId={} newVariantCount={}", saved.getId(), newVariantStocks.size());
        }
        ProductAdminResponse response = toAdminResponse(saved);
        log.info("[1917] Update product completed productId={}", saved.getId());
        return response;
    }

    @Transactional
    public ProductAdminResponse updateStatus(Long id, ProductStatus status) {
        log.info("[1918] Update product status id={} newStatus={}", id, status);
        Product product = findProduct(id);
        ProductStatus oldStatus = product.getStatus();
        product.setStatus(status);
        ProductAdminResponse response = toAdminResponse(productRepository.save(product));
        log.info("[1919] Product status changed productId={} oldStatus={} newStatus={}", id, oldStatus, status);
        return response;
    }

    @Transactional
    public void delete(Long id) {
        log.info("[1920] Delete product id={}", id);
        Product product = findProduct(id);
        List<Long> variantIds = product.getVariants().stream().map(ProductVariant::getId).filter(Objects::nonNull).toList();
        if (!variantIds.isEmpty()
                && (inventoryTransactionRepository.existsByProductVariantIdIn(variantIds)
                        || purchaseItemRepository.existsByProductVariantIdIn(variantIds)
                        || damageRecordRepository.existsByProductVariantIdIn(variantIds))) {
            log.error("[1921] Delete product failed, inventory history exists productId={} variantIds={}", id, variantIds);
            throw new ConflictException("Cannot delete a product with inventory history - deactivate or archive it instead");
        }
        productRepository.delete(product);
        log.info("[1922] Product deleted productId={}", id);
    }

    private Product findProduct(Long id) {
        return productRepository.findById(id).orElseThrow(() -> {
            log.error("[1923] Product not found id={}", id);
            return new NotFoundException("Product not found: " + id);
        });
    }

    private void applyProductFields(Product product, ProductAdminRequest request) {
        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> {
                    log.error("[1924] Category not found categoryId={}", request.categoryId());
                    return new NotFoundException("Category not found: " + request.categoryId());
                });
        Brand brand = null;
        if (request.brandId() != null) {
            brand = brandRepository.findById(request.brandId())
                    .orElseThrow(() -> {
                        log.error("[1925] Brand not found brandId={}", request.brandId());
                        return new NotFoundException("Brand not found: " + request.brandId());
                    });
        }
        Material material = materialRepository.findById(request.materialId())
                .orElseThrow(() -> {
                    log.error("[1926] Material not found materialId={}", request.materialId());
                    return new NotFoundException("Material not found: " + request.materialId());
                });
        SubCategory subCategory = null;
        if (request.subCategoryId() != null) {
            subCategory = subCategoryRepository.findById(request.subCategoryId())
                    .orElseThrow(() -> {
                        log.error("[1927] Sub-category not found subCategoryId={}", request.subCategoryId());
                        return new NotFoundException("Sub-category not found: " + request.subCategoryId());
                    });
        }

        product.setCategory(category);
        product.setSubCategory(subCategory);
        product.setBrand(brand);
        product.setMaterial(material);
        product.setName(request.name());
        product.setSlug(request.slug());
        product.setDescription(request.description());
        product.setStatus(request.status() != null ? request.status() : ProductStatus.ACTIVE);
        product.setBaseSku(request.baseSku());
        product.setBaseSellingPrice(request.baseSellingPrice());
        product.setBaseCostPrice(request.baseCostPrice());
        log.info("[1928] Product fields applied slug={} categoryId={} brandId={} materialId={}",
                request.slug(), request.categoryId(), request.brandId(), request.materialId());
    }

    /** A brand-new variant created in this request, paired with the initial stock it needs an audit-trail entry for. */
    private record NewVariantStock(ProductVariant variant, int quantity) {}

    private List<NewVariantStock> applyVariants(Product product, List<VariantAdminRequest> variantRequests) {
        List<NewVariantStock> newVariantStocks = new ArrayList<>();
        if (variantRequests == null) {
            return newVariantStocks;
        }
        Set<Long> existingIds = product.getVariants().stream()
                .map(ProductVariant::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(HashSet::new));
        Set<Long> keepIds = new HashSet<>();

        for (VariantAdminRequest vr : variantRequests) {
            Size size = sizeRepository.findById(vr.sizeId())
                    .orElseThrow(() -> {
                        log.error("[1929] Size not found sizeId={}", vr.sizeId());
                        return new NotFoundException("Size not found: " + vr.sizeId());
                    });
            Color color = colorRepository.findById(vr.colorId())
                    .orElseThrow(() -> {
                        log.error("[1930] Color not found colorId={}", vr.colorId());
                        return new NotFoundException("Color not found: " + vr.colorId());
                    });

            ProductVariant variant;
            boolean isNew = vr.id() == null;
            if (!isNew) {
                variant = product.getVariants().stream()
                        .filter(v -> v.getId().equals(vr.id()))
                        .findFirst()
                        .orElseThrow(() -> {
                            log.error("[1931] Variant not found on product variantId={}", vr.id());
                            return new NotFoundException("Variant not found on this product: " + vr.id());
                        });
            } else {
                if (productVariantRepository.existsBySkuIgnoreCase(vr.sku())) {
                    log.error("[1932] Variant SKU already exists sku={}", vr.sku());
                    throw new ConflictException("A variant with SKU " + vr.sku() + " already exists");
                }
                variant = new ProductVariant();
                product.addVariant(variant);
            }

            variant.setSku(vr.sku());
            variant.setSize(size);
            variant.setColor(color);
            variant.setSellingPrice(vr.sellingPrice());
            variant.setDiscountPercent(vr.discountPercent() != null ? vr.discountPercent() : BigDecimal.ZERO);
            variant.setCostPrice(vr.costPrice());
            variant.setLowStockThreshold(vr.lowStockThreshold());
            variant.setActive(vr.active() == null || vr.active());

            // stockQuantity is ONLY used for a brand-new variant's initial stock. For an
            // existing variant it is completely ignored here - stock changes for existing
            // variants can only happen through StockService (adjust/damage endpoints), never
            // silently through this form, so the audit trail is never bypassed.
            if (isNew) {
                int initialQuantity = vr.stockQuantity() != null ? vr.stockQuantity() : 0;
                variant.setStockQuantity(initialQuantity);
                newVariantStocks.add(new NewVariantStock(variant, initialQuantity));
                log.info("[1933] New variant created sku={} initialQuantity={}", vr.sku(), initialQuantity);
            } else {
                log.info("[1934] Existing variant updated variantId={} sku={}", vr.id(), vr.sku());
            }

            if (vr.images() != null) {
                variant.getImages().clear();
                // The submitted list's own order is always what's persisted as displayOrder -
                // a client-sent displayOrder value (if any) is ignored, never trusted for
                // ordering. `primary` IS trusted, but normalized defensively: if the client
                // marked none (or, from a stale double-submit, more than one) as primary, the
                // first image in the list wins so exactly one image is ever primary.
                boolean primaryAssigned = false;
                int order = 0;
                for (VariantImageRequest imgReq : vr.images()) {
                    ProductImage image = new ProductImage();
                    image.setUrl(imgReq.url());
                    image.setDisplayOrder(order++);
                    boolean wantsPrimary = Boolean.TRUE.equals(imgReq.primary());
                    image.setPrimary(wantsPrimary && !primaryAssigned);
                    if (image.isPrimary()) {
                        primaryAssigned = true;
                    }
                    variant.addImage(image);
                }
                if (!primaryAssigned && !variant.getImages().isEmpty()) {
                    variant.getImages().get(0).setPrimary(true);
                }
            }

            if (variant.getId() != null) {
                keepIds.add(variant.getId());
            }
        }

        // Remove variants that existed before this request but weren't included in it.
        Set<Long> removedIds = existingIds.stream().filter(eid -> !keepIds.contains(eid)).collect(Collectors.toSet());
        product.getVariants().removeIf(v -> v.getId() != null && removedIds.contains(v.getId()));
        if (!removedIds.isEmpty()) {
            log.info("[1935] Variants removed from product removedVariantIds={}", removedIds);
        }

        return newVariantStocks;
    }

    private void recordInitialStockTransactions(List<NewVariantStock> newVariantStocks, Long actingUserId) {
        for (NewVariantStock nv : newVariantStocks) {
            // IDENTITY generation means nv.variant().getId() is already populated here - the
            // cascade PERSIST from productRepository.save() above issued the INSERT eagerly.
            log.info("[1936] Recording initial stock transaction variantId={} quantity={}", nv.variant().getId(), nv.quantity());
            stockService.recordInitialStock(nv.variant(), nv.quantity(), actingUserId);
        }
    }

    private ProductSummaryResponse toSummary(Product product) {
        List<ProductVariant> variants = product.getVariants();
        List<ProductVariant> considered = variants.stream().filter(ProductVariant::isActive).toList();
        if (considered.isEmpty()) {
            considered = variants;
        }

        BigDecimal minPrice = considered.stream().map(ProductVariant::getSellingPrice).min(Comparator.naturalOrder()).orElse(BigDecimal.ZERO);
        BigDecimal maxPrice = considered.stream().map(ProductVariant::getSellingPrice).max(Comparator.naturalOrder()).orElse(BigDecimal.ZERO);
        BigDecimal discountPercent = considered.stream().map(ProductVariant::getDiscountPercent).max(Comparator.naturalOrder()).orElse(BigDecimal.ZERO);
        boolean inStock = considered.stream().anyMatch(v -> v.getStockQuantity() > 0);
        String primaryImageUrl = considered.stream()
                .sorted(Comparator.comparing(ProductVariant::getId))
                .flatMap(v -> v.getImages().stream().sorted(ProductImage.displayOrderComparator()))
                .map(ProductImage::getUrl)
                .findFirst()
                .orElse(null);

        return new ProductSummaryResponse(
                product.getId(),
                product.getSlug(),
                product.getName(),
                product.getBrand() != null ? product.getBrand().getName() : null,
                primaryImageUrl,
                minPrice,
                maxPrice,
                discountPercent,
                product.getCategory().getName(),
                inStock);
    }

    private ProductDetailResponse toDetail(Product product) {
        List<VariantResponse> variants = product.getVariants().stream()
                .filter(ProductVariant::isActive)
                .map(this::toVariantResponse)
                .toList();
        return new ProductDetailResponse(
                product.getId(),
                product.getSlug(),
                product.getName(),
                product.getDescription(),
                product.getCategory().getName(),
                product.getSubCategory() != null ? product.getSubCategory().getName() : null,
                product.getBrand() != null ? product.getBrand().getName() : null,
                product.getMaterial().getName(),
                variants);
    }

    private VariantResponse toVariantResponse(ProductVariant v) {
        List<String> images = v.getImages().stream()
                .sorted(ProductImage.displayOrderComparator())
                .map(ProductImage::getUrl)
                .toList();
        return new VariantResponse(
                v.getId(), v.getSku(), v.getSize().getName(), v.getColor().getName(), v.getColor().getHexCode(),
                v.getSellingPrice(), v.getDiscountPercent(), v.getStockQuantity(), images);
    }

    private ProductAdminResponse toAdminResponse(Product product) {
        List<VariantAdminResponse> variants = product.getVariants().stream().map(this::toVariantAdminResponse).toList();
        return new ProductAdminResponse(
                product.getId(),
                product.getCategory().getId(),
                product.getCategory().getName(),
                product.getSubCategory() != null ? product.getSubCategory().getId() : null,
                product.getSubCategory() != null ? product.getSubCategory().getName() : null,
                product.getBrand() != null ? product.getBrand().getId() : null,
                product.getBrand() != null ? product.getBrand().getName() : null,
                product.getMaterial().getId(),
                product.getMaterial().getName(),
                product.getName(),
                product.getSlug(),
                product.getDescription(),
                product.getStatus(),
                product.getBaseSku(),
                product.getBaseSellingPrice(),
                product.getBaseCostPrice(),
                variants);
    }

    private ProductAdminSummaryResponse toAdminSummary(Product product) {
        List<ProductVariant> variants = product.getVariants();
        int totalStock = variants.stream().mapToInt(ProductVariant::getStockQuantity).sum();
        return new ProductAdminSummaryResponse(
                product.getId(),
                product.getName(),
                product.getSlug(),
                product.getBaseSku(),
                product.getCategory().getName(),
                product.getBrand() != null ? product.getBrand().getName() : null,
                product.getStatus(),
                variants.size(),
                totalStock,
                product.getUpdatedAt());
    }

    private VariantAdminResponse toVariantAdminResponse(ProductVariant v) {
        List<VariantImageResponse> images = v.getImages().stream()
                .sorted(ProductImage.displayOrderComparator())
                .map(img -> new VariantImageResponse(img.getId(), img.getUrl(), img.getDisplayOrder(), img.isPrimary()))
                .toList();
        return new VariantAdminResponse(
                v.getId(),
                v.getSku(),
                v.getSize().getId(),
                v.getSize().getName(),
                v.getColor().getId(),
                v.getColor().getName(),
                v.getSellingPrice(),
                v.getDiscountPercent(),
                v.getCostPrice(),
                v.getStockQuantity(),
                v.getReservedQuantity(),
                v.getDamagedQuantity(),
                v.getAvailableQuantity(),
                v.getLowStockThreshold(),
                v.isActive(),
                images);
    }
}
