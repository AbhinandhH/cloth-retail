package com.clothingretail.masterdata;

import com.clothingretail.masterdata.dto.CategoryResponse;
import com.clothingretail.masterdata.dto.ColorResponse;
import com.clothingretail.masterdata.dto.SizeResponse;
import com.clothingretail.masterdata.dto.SubCategoryResponse;
import com.clothingretail.masterdata.dto.VendorResponse;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public, read-only master-data lookups used by the storefront for filters
 * and display (categories, sizes, colors, vendors). Active-only, ordered by
 * displayOrder then name - see each *Service#listPublic for the query.
 */
@RestController
public class PublicMasterDataController {

    private final CategoryService categoryService;
    private final SubCategoryService subCategoryService;
    private final SizeService sizeService;
    private final ColorService colorService;
    private final VendorService vendorService;

    public PublicMasterDataController(
            CategoryService categoryService,
            SubCategoryService subCategoryService,
            SizeService sizeService,
            ColorService colorService,
            VendorService vendorService) {
        this.categoryService = categoryService;
        this.subCategoryService = subCategoryService;
        this.sizeService = sizeService;
        this.colorService = colorService;
        this.vendorService = vendorService;
    }

    @GetMapping("/api/categories")
    public List<CategoryResponse> categories() {
        return categoryService.listPublic();
    }

    @GetMapping("/api/sub-categories")
    public List<SubCategoryResponse> subCategories(@RequestParam(required = false) Long categoryId) {
        return subCategoryService.listPublic(categoryId);
    }

    @GetMapping("/api/sizes")
    public List<SizeResponse> sizes() {
        return sizeService.listPublic();
    }

    @GetMapping("/api/colors")
    public List<ColorResponse> colors() {
        return colorService.listPublic();
    }

    @GetMapping("/api/vendors")
    public List<VendorResponse> vendors() {
        return vendorService.listPublic();
    }
}
