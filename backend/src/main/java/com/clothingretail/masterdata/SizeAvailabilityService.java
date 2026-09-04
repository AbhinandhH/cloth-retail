package com.clothingretail.masterdata;

import com.clothingretail.common.NotFoundException;
import com.clothingretail.masterdata.dto.AvailableSizeResponse;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Computes which sizes are pickable for a category: the deduped, order-respecting union of
 * sizes across every active {@link SizeGroup} associated with the category, falling back to
 * the full active global {@link Size} list when the category has no size groups at all - this
 * fallback is what keeps the pre-existing seeded demo products working once size groups ship,
 * since none of their categories have one configured.
 */
@Service
@Transactional(readOnly = true)
public class SizeAvailabilityService {

    private final CategoryRepository categoryRepository;
    private final SizeGroupRepository sizeGroupRepository;
    private final SizeRepository sizeRepository;

    public SizeAvailabilityService(
            CategoryRepository categoryRepository, SizeGroupRepository sizeGroupRepository, SizeRepository sizeRepository) {
        this.categoryRepository = categoryRepository;
        this.sizeGroupRepository = sizeGroupRepository;
        this.sizeRepository = sizeRepository;
    }

    public List<AvailableSizeResponse> availableSizesForCategory(Long categoryId) {
        if (!categoryRepository.existsById(categoryId)) {
            throw new NotFoundException("Category not found: " + categoryId);
        }

        List<SizeGroup> groups = sizeGroupRepository.findActiveByCategoryId(categoryId);
        if (groups.isEmpty()) {
            return sizeRepository.findByActiveTrueOrderByDisplayOrderAscNameAsc().stream()
                    .map(s -> new AvailableSizeResponse(s.getId(), s.getName()))
                    .toList();
        }

        // findActiveByCategoryId already orders groups by displayOrder; within each group,
        // order by the join row's own displayOrder. First occurrence wins on duplicates.
        LinkedHashSet<Long> seenSizeIds = new LinkedHashSet<>();
        List<AvailableSizeResponse> result = new ArrayList<>();
        for (SizeGroup group : groups) {
            group.getSizeGroupSizes().stream()
                    .sorted(Comparator.comparing(SizeGroupSize::getDisplayOrder))
                    .forEach(sgs -> {
                        Size size = sgs.getSize();
                        if (seenSizeIds.add(size.getId())) {
                            result.add(new AvailableSizeResponse(size.getId(), size.getName()));
                        }
                    });
        }
        return result;
    }
}
