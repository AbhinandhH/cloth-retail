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
 * displayOrder then name.
 */
@RestController
public class PublicMasterDataController {

    private final CategoryRepository categoryRepository;
    private final SubCategoryRepository subCategoryRepository;
    private final SizeRepository sizeRepository;
    private final ColorRepository colorRepository;
    private final VendorRepository vendorRepository;

    public PublicMasterDataController(
            CategoryRepository categoryRepository,
            SubCategoryRepository subCategoryRepository,
            SizeRepository sizeRepository,
            ColorRepository colorRepository,
            VendorRepository vendorRepository) {
        this.categoryRepository = categoryRepository;
        this.subCategoryRepository = subCategoryRepository;
        this.sizeRepository = sizeRepository;
        this.colorRepository = colorRepository;
        this.vendorRepository = vendorRepository;
    }

    @GetMapping("/api/categories")
    public List<CategoryResponse> categories() {
        return categoryRepository.findByActiveTrueOrderByDisplayOrderAscNameAsc().stream()
                .map(c -> new CategoryResponse(c.getId(), c.getName(), c.getSlug()))
                .toList();
    }

    @GetMapping("/api/sub-categories")
    public List<SubCategoryResponse> subCategories(@RequestParam(required = false) Long categoryId) {
        List<SubCategory> subCategories = categoryId != null
                ? subCategoryRepository.findByCategoryIdAndActiveTrueOrderByDisplayOrderAscNameAsc(categoryId)
                : subCategoryRepository.findByActiveTrueOrderByDisplayOrderAscNameAsc();
        return subCategories.stream()
                .map(sc -> new SubCategoryResponse(sc.getId(), sc.getName(), sc.getSlug(), sc.getCategory().getId()))
                .toList();
    }

    @GetMapping("/api/sizes")
    public List<SizeResponse> sizes() {
        return sizeRepository.findByActiveTrueOrderByDisplayOrderAscNameAsc().stream()
                .map(s -> new SizeResponse(s.getId(), s.getName()))
                .toList();
    }

    @GetMapping("/api/colors")
    public List<ColorResponse> colors() {
        return colorRepository.findByActiveTrueOrderByDisplayOrderAscNameAsc().stream()
                .map(c -> new ColorResponse(c.getId(), c.getName(), c.getHexCode()))
                .toList();
    }

    @GetMapping("/api/vendors")
    public List<VendorResponse> vendors() {
        return vendorRepository.findByActiveTrueOrderByNameAsc().stream()
                .map(v -> new VendorResponse(v.getId(), v.getName()))
                .toList();
    }
}
