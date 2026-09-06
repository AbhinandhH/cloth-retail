package com.clothingretail.masterdata;

import com.clothingretail.common.NotFoundException;
import com.clothingretail.masterdata.dto.AvailableSizeResponse;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import lombok.extern.log4j.Log4j2;
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
@Log4j2
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
        log.info("[1435] Resolving available sizes for categoryId={}", categoryId);
        if (!categoryRepository.existsById(categoryId)) {
            log.error("[1436] Cannot resolve available sizes - categoryId={} not found", categoryId);
            throw new NotFoundException("Category not found: " + categoryId);
        }

        List<SizeGroup> groups = sizeGroupRepository.findActiveByCategoryId(categoryId);
        if (groups.isEmpty()) {
            List<AvailableSizeResponse> fallback = sizeRepository.findByActiveTrueOrderByDisplayOrderAscNameAsc().stream()
                    .map(s -> new AvailableSizeResponse(s.getId(), s.getName()))
                    .toList();
            log.info("[1437] No active size groups for categoryId={} - falling back to {} sizes from global active list", categoryId, fallback.size());
            return fallback;
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
        log.info("[1438] Resolved {} sizes for categoryId={} from {} active size group(s)", result.size(), categoryId, groups.size());
        return result;
    }
}
