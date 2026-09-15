package com.clothingretail.product.service;

import com.clothingretail.product.ProductStatus;
import com.clothingretail.product.dto.ProductAdminRequest;
import com.clothingretail.product.dto.ProductAdminResponse;
import com.clothingretail.product.dto.ProductAdminSummaryResponse;
import com.clothingretail.product.dto.ProductDetailResponse;
import com.clothingretail.product.dto.ProductSummaryResponse;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ProductService {

    Page<ProductSummaryResponse> list(
            Long categoryId,
            Long subCategoryId,
            Long sizeId,
            Long colorId,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            String q,
            Pageable pageable);

    List<ProductSummaryResponse> listByIds(List<Long> ids);

    ProductDetailResponse getBySlug(String slug);

    Page<ProductAdminSummaryResponse> listAdmin(String q, Long categoryId, ProductStatus status, Pageable pageable);

    ProductAdminResponse getAdmin(Long id);

    ProductAdminResponse create(ProductAdminRequest request, Long actingUserId);

    ProductAdminResponse update(Long id, ProductAdminRequest request, Long actingUserId);

    ProductAdminResponse updateStatus(Long id, ProductStatus status);

    void delete(Long id);
}
