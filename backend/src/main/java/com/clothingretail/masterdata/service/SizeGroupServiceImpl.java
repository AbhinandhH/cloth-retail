package com.clothingretail.masterdata.service;

import com.clothingretail.common.AuditorNameResolver;
import com.clothingretail.common.NotFoundException;
import com.clothingretail.masterdata.Category;
import com.clothingretail.masterdata.Size;
import com.clothingretail.masterdata.SizeGroup;
import com.clothingretail.masterdata.SizeGroupSize;
import com.clothingretail.masterdata.dto.SizeGroupAdminRequest;
import com.clothingretail.masterdata.dto.SizeGroupAdminResponse;
import com.clothingretail.masterdata.dto.SizeGroupSizeResponse;
import com.clothingretail.masterdata.repository.CategoryRepository;
import com.clothingretail.masterdata.repository.SizeGroupRepository;
import com.clothingretail.masterdata.repository.SizeRepository;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional(readOnly = true)
@Log4j2
public class SizeGroupServiceImpl implements SizeGroupService {

    private final SizeGroupRepository repository;
    private final CategoryRepository categoryRepository;
    private final SizeRepository sizeRepository;
    private final AuditorNameResolver auditorNameResolver;

    public SizeGroupServiceImpl(
            SizeGroupRepository repository,
            CategoryRepository categoryRepository,
            SizeRepository sizeRepository,
            AuditorNameResolver auditorNameResolver) {
        this.repository = repository;
        this.categoryRepository = categoryRepository;
        this.sizeRepository = sizeRepository;
        this.auditorNameResolver = auditorNameResolver;
    }

    @Override
    public List<SizeGroupAdminResponse> listAdmin(String q, Boolean active) {
        log.info("[1439] Listing size groups query={} active={}", q, active);
        List<SizeGroup> groups = repository.findAll().stream()
                .filter(g -> matchesQuery(g, q))
                .filter(g -> active == null || g.isActive() == active)
                .toList();
        Map<Long, String> names = auditorNameResolver.resolveNames(
                groups.stream().flatMap(g -> Stream.of(g.getCreatedBy(), g.getUpdatedBy())).toList());
        return groups.stream().map(g -> toResponse(g, names)).toList();
    }

    @Override
    public SizeGroupAdminResponse getAdmin(Long id) {
        log.info("[1440] Fetching size group id={}", id);
        SizeGroup group = find(id);
        return toResponse(group, auditorNameResolver.resolveNames(group.getCreatedBy(), group.getUpdatedBy()));
    }

    @Override
    @Transactional
    public SizeGroupAdminResponse create(SizeGroupAdminRequest request) {
        log.info("[1441] Creating size group name={} displayOrder={} active={} categoryIds={} sizeIds={}", request.name(), request.displayOrder(), request.active(), request.categoryIds(), request.sizeIds());
        SizeGroup group = new SizeGroup();
        apply(group, request);
        SizeGroup saved = repository.save(group);
        return toResponse(saved, auditorNameResolver.resolveNames(saved.getCreatedBy(), saved.getUpdatedBy()));
    }

    @Override
    @Transactional
    public SizeGroupAdminResponse update(Long id, SizeGroupAdminRequest request) {
        log.info("[1442] Updating size group id={} name={} displayOrder={} active={} categoryIds={} sizeIds={}", id, request.name(), request.displayOrder(), request.active(), request.categoryIds(), request.sizeIds());
        SizeGroup group = find(id);
        apply(group, request);
        SizeGroup saved = repository.save(group);
        return toResponse(saved, auditorNameResolver.resolveNames(saved.getCreatedBy(), saved.getUpdatedBy()));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        log.info("[1443] Deleting size group id={}", id);
        // No delete-guard needed: nothing holds a hard FK to a SizeGroup (ProductVariant.size
        // points directly at the flat Size table) - see the class-level Javadoc.
        repository.delete(find(id));
    }

    private SizeGroup find(Long id) {
        return repository.findById(id).orElseThrow(() -> {
            log.error("[1444] Size group not found id={}", id);
            return new NotFoundException("Size group not found: " + id);
        });
    }

    private boolean matchesQuery(SizeGroup group, String q) {
        return !StringUtils.hasText(q) || group.getName().toLowerCase().contains(q.toLowerCase());
    }

    private void apply(SizeGroup group, SizeGroupAdminRequest request) {
        group.setName(request.name());
        group.setDescription(request.description());
        group.setDisplayOrder(request.displayOrder() != null ? request.displayOrder() : 0);
        group.setActive(request.active());

        List<Long> categoryIds = request.categoryIds() != null ? request.categoryIds() : List.of();
        Set<Category> categories = new HashSet<>(categoryRepository.findAllById(categoryIds));
        if (categories.size() != new HashSet<>(categoryIds).size()) {
            log.error("[1445] Cannot save size group - {} of {} requested categoryIds not found: {}", categories.size(), new HashSet<>(categoryIds).size(), categoryIds);
            throw new NotFoundException("One or more categories were not found");
        }
        group.setCategories(categories);

        List<Long> sizeIds = request.sizeIds() != null ? request.sizeIds() : List.of();
        Map<Long, Size> sizesById =
                sizeRepository.findAllById(sizeIds).stream().collect(Collectors.toMap(Size::getId, s -> s));
        if (sizesById.size() != new HashSet<>(sizeIds).size()) {
            log.error("[1446] Cannot save size group - {} of {} requested sizeIds not found: {}", sizesById.size(), new HashSet<>(sizeIds).size(), sizeIds);
            throw new NotFoundException("One or more sizes were not found");
        }

        group.getSizeGroupSizes().clear();
        int order = 0;
        for (Long sizeId : sizeIds) {
            SizeGroupSize sgs = new SizeGroupSize();
            sgs.setSize(sizesById.get(sizeId));
            sgs.setDisplayOrder(order++);
            group.addSizeGroupSize(sgs);
        }
        log.info("[1447] Assigned display order for {} size(s) in size group", sizeIds.size());
    }

    private SizeGroupAdminResponse toResponse(SizeGroup g, Map<Long, String> names) {
        List<Long> categoryIds = g.getCategories().stream().map(Category::getId).sorted().toList();
        List<String> categoryNames =
                g.getCategories().stream().sorted(Comparator.comparing(Category::getName)).map(Category::getName).toList();
        List<SizeGroupSizeResponse> sizes = g.getSizeGroupSizes().stream()
                .sorted(Comparator.comparing(SizeGroupSize::getDisplayOrder))
                .map(sgs -> new SizeGroupSizeResponse(sgs.getSize().getId(), sgs.getSize().getName(), sgs.getDisplayOrder()))
                .toList();
        return new SizeGroupAdminResponse(
                g.getId(),
                g.getName(),
                g.getDescription(),
                g.getDisplayOrder(),
                g.isActive(),
                categoryIds,
                categoryNames,
                sizes,
                names.get(g.getCreatedBy()),
                names.get(g.getUpdatedBy()),
                g.getCreatedAt(),
                g.getUpdatedAt());
    }
}
