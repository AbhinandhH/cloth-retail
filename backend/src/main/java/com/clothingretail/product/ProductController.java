package com.clothingretail.product;

import com.clothingretail.common.PageResponse;
import com.clothingretail.product.dto.ProductDetailResponse;
import com.clothingretail.product.dto.ProductSummaryResponse;
import java.math.BigDecimal;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public PageResponse<ProductSummaryResponse> list(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long subCategoryId,
            @RequestParam(required = false) Long sizeId,
            @RequestParam(required = false) Long colorId,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) String q,
            @PageableDefault(size = 20) Pageable pageable) {
        return PageResponse.of(
                productService.list(categoryId, subCategoryId, sizeId, colorId, minPrice, maxPrice, q, pageable));
    }

    @GetMapping("/{slug}")
    public ProductDetailResponse detail(@PathVariable String slug) {
        return productService.getBySlug(slug);
    }
}
