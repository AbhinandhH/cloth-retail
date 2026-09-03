package com.clothingretail.product;

import com.clothingretail.common.ConflictException;
import com.clothingretail.common.NotFoundException;
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
import com.clothingretail.product.dto.ProductDetailResponse;
import com.clothingretail.product.dto.ProductSummaryResponse;
import com.clothingretail.product.dto.VariantAdminRequest;
import com.clothingretail.product.dto.VariantAdminResponse;
import com.clothingretail.product.dto.VariantResponse;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final CategoryRepository categoryRepository;
    private final SubCategoryRepository subCategoryRepository;
    private final BrandRepository brandRepository;
    private final MaterialRepository materialRepository;
    private final SizeRepository sizeRepository;
    private final ColorRepository colorRepository;

    public ProductService(
            ProductRepository productRepository,
            ProductVariantRepository productVariantRepository,
            CategoryRepository categoryRepository,
            SubCategoryRepository subCategoryRepository,
            BrandRepository brandRepository,
            MaterialRepository materialRepository,
            SizeRepository sizeRepository,
            ColorRepository colorRepository) {
        this.productRepository = productRepository;
        this.productVariantRepository = productVariantRepository;
        this.categoryRepository = categoryRepository;
        this.subCategoryRepository = subCategoryRepository;
        this.brandRepository = brandRepository;
        this.materialRepository = materialRepository;
        this.sizeRepository = sizeRepository;
        this.colorRepository = colorRepository;
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
        var spec = ProductSpecifications.filter(categoryId, subCategoryId, sizeId, colorId, minPrice, maxPrice, q, true);
        Page<Product> page = productRepository.findAll(spec, pageable);
        return page.map(this::toSummary);
    }

    public ProductDetailResponse getBySlug(String slug) {
        Product product = productRepository.findBySlugAndActiveTrue(slug)
                .orElseThrow(() -> new NotFoundException("Product not found: " + slug));
        return toDetail(product);
    }

    public List<ProductAdminResponse> listAdmin() {
        return productRepository.findAll().stream().map(this::toAdminResponse).toList();
    }

    public ProductAdminResponse getAdmin(Long id) {
        return toAdminResponse(findProduct(id));
    }

    @Transactional
    public ProductAdminResponse create(ProductAdminRequest request) {
        if (productRepository.existsBySlugIgnoreCase(request.slug())) {
            throw new ConflictException("A product with this slug already exists");
        }
        Product product = new Product();
        applyProductFields(product, request);
        applyVariants(product, request.variants());
        return toAdminResponse(productRepository.save(product));
    }

    @Transactional
    public ProductAdminResponse update(Long id, ProductAdminRequest request) {
        Product product = findProduct(id);
        applyProductFields(product, request);
        applyVariants(product, request.variants());
        return toAdminResponse(productRepository.save(product));
    }

    @Transactional
    public void delete(Long id) {
        Product product = findProduct(id);
        productRepository.delete(product);
    }

    private Product findProduct(Long id) {
        return productRepository.findById(id).orElseThrow(() -> new NotFoundException("Product not found: " + id));
    }

    private void applyProductFields(Product product, ProductAdminRequest request) {
        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new NotFoundException("Category not found: " + request.categoryId()));
        Brand brand = brandRepository.findById(request.brandId())
                .orElseThrow(() -> new NotFoundException("Brand not found: " + request.brandId()));
        Material material = materialRepository.findById(request.materialId())
                .orElseThrow(() -> new NotFoundException("Material not found: " + request.materialId()));
        SubCategory subCategory = null;
        if (request.subCategoryId() != null) {
            subCategory = subCategoryRepository.findById(request.subCategoryId())
                    .orElseThrow(() -> new NotFoundException("Sub-category not found: " + request.subCategoryId()));
        }

        product.setCategory(category);
        product.setSubCategory(subCategory);
        product.setBrand(brand);
        product.setMaterial(material);
        product.setName(request.name());
        product.setSlug(request.slug());
        product.setDescription(request.description());
        product.setActive(request.active() == null || request.active());
    }

    private void applyVariants(Product product, List<VariantAdminRequest> variantRequests) {
        if (variantRequests == null) {
            return;
        }
        Set<Long> existingIds = product.getVariants().stream()
                .map(ProductVariant::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(HashSet::new));
        Set<Long> keepIds = new HashSet<>();

        for (VariantAdminRequest vr : variantRequests) {
            Size size = sizeRepository.findById(vr.sizeId())
                    .orElseThrow(() -> new NotFoundException("Size not found: " + vr.sizeId()));
            Color color = colorRepository.findById(vr.colorId())
                    .orElseThrow(() -> new NotFoundException("Color not found: " + vr.colorId()));

            ProductVariant variant;
            if (vr.id() != null) {
                variant = product.getVariants().stream()
                        .filter(v -> v.getId().equals(vr.id()))
                        .findFirst()
                        .orElseThrow(() -> new NotFoundException("Variant not found on this product: " + vr.id()));
            } else {
                if (productVariantRepository.existsBySkuIgnoreCase(vr.sku())) {
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
            variant.setStockQuantity(vr.stockQuantity() != null ? vr.stockQuantity() : 0);
            variant.setActive(vr.active() == null || vr.active());

            if (vr.images() != null) {
                variant.getImages().clear();
                int order = 0;
                for (String url : vr.images()) {
                    ProductImage image = new ProductImage();
                    image.setUrl(url);
                    image.setDisplayOrder(order++);
                    variant.addImage(image);
                }
            }

            if (variant.getId() != null) {
                keepIds.add(variant.getId());
            }
        }

        // Remove variants that existed before this request but weren't included in it.
        product.getVariants().removeIf(v -> v.getId() != null && existingIds.contains(v.getId()) && !keepIds.contains(v.getId()));
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
                .flatMap(v -> v.getImages().stream().sorted(Comparator.comparing(ProductImage::getDisplayOrder)))
                .map(ProductImage::getUrl)
                .findFirst()
                .orElse(null);

        return new ProductSummaryResponse(
                product.getId(),
                product.getSlug(),
                product.getName(),
                product.getBrand().getName(),
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
                product.getBrand().getName(),
                product.getMaterial().getName(),
                variants);
    }

    private VariantResponse toVariantResponse(ProductVariant v) {
        List<String> images = v.getImages().stream()
                .sorted(Comparator.comparing(ProductImage::getDisplayOrder))
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
                product.getBrand().getId(),
                product.getBrand().getName(),
                product.getMaterial().getId(),
                product.getMaterial().getName(),
                product.getName(),
                product.getSlug(),
                product.getDescription(),
                product.isActive(),
                variants);
    }

    private VariantAdminResponse toVariantAdminResponse(ProductVariant v) {
        List<String> images = v.getImages().stream()
                .sorted(Comparator.comparing(ProductImage::getDisplayOrder))
                .map(ProductImage::getUrl)
                .toList();
        return new VariantAdminResponse(
                v.getId(), v.getSku(), v.getSize().getId(), v.getSize().getName(), v.getColor().getId(), v.getColor().getName(),
                v.getSellingPrice(), v.getDiscountPercent(), v.getStockQuantity(), v.isActive(), images);
    }
}
