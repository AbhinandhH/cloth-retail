package com.clothingretail.masterdata;

import com.clothingretail.common.AuditorNameResolver;
import com.clothingretail.common.ConflictException;
import com.clothingretail.common.NotFoundException;
import com.clothingretail.masterdata.dto.SizeAdminRequest;
import com.clothingretail.masterdata.dto.SizeAdminResponse;
import com.clothingretail.masterdata.dto.SizeResponse;
import com.clothingretail.product.ProductVariantRepository;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional(readOnly = true)
public class SizeService {

    private final SizeRepository repository;
    private final ProductVariantRepository productVariantRepository;
    private final AuditorNameResolver auditorNameResolver;

    public SizeService(
            SizeRepository repository, ProductVariantRepository productVariantRepository, AuditorNameResolver auditorNameResolver) {
        this.repository = repository;
        this.productVariantRepository = productVariantRepository;
        this.auditorNameResolver = auditorNameResolver;
    }

    public List<SizeAdminResponse> listAdmin(String q, Boolean active) {
        List<Size> sizes = repository.findAll().stream()
                .filter(s -> matchesQuery(s, q))
                .filter(s -> active == null || s.isActive() == active)
                .toList();
        Map<Long, String> names = auditorNameResolver.resolveNames(
                sizes.stream().flatMap(s -> Stream.of(s.getCreatedBy(), s.getUpdatedBy())).toList());
        return sizes.stream().map(s -> toResponse(s, names)).toList();
    }

    public SizeAdminResponse getAdmin(Long id) {
        Size size = find(id);
        return toResponse(size, auditorNameResolver.resolveNames(size.getCreatedBy(), size.getUpdatedBy()));
    }

    public List<SizeResponse> listPublic() {
        return repository.findByActiveTrueOrderByDisplayOrderAscNameAsc().stream()
                .map(s -> new SizeResponse(s.getId(), s.getName()))
                .toList();
    }

    @Transactional
    public SizeAdminResponse create(SizeAdminRequest request) {
        if (repository.existsByNameIgnoreCase(request.name())) {
            throw new ConflictException("A size with this name already exists");
        }
        Size size = new Size();
        apply(size, request);
        Size saved = repository.save(size);
        return toResponse(saved, auditorNameResolver.resolveNames(saved.getCreatedBy(), saved.getUpdatedBy()));
    }

    @Transactional
    public SizeAdminResponse update(Long id, SizeAdminRequest request) {
        Size size = find(id);
        apply(size, request);
        Size saved = repository.save(size);
        return toResponse(saved, auditorNameResolver.resolveNames(saved.getCreatedBy(), saved.getUpdatedBy()));
    }

    @Transactional
    public void delete(Long id) {
        Size size = find(id);
        long usageCount = productVariantRepository.countBySizeId(id);
        if (usageCount > 0) {
            throw new ConflictException(
                    "This size is currently used by " + usageCount + " product variants and cannot be deleted. Deactivate it instead.");
        }
        repository.delete(size);
    }

    private Size find(Long id) {
        return repository.findById(id).orElseThrow(() -> new NotFoundException("Size not found: " + id));
    }

    private boolean matchesQuery(Size size, String q) {
        return !StringUtils.hasText(q) || size.getName().toLowerCase().contains(q.toLowerCase());
    }

    private void apply(Size size, SizeAdminRequest request) {
        size.setName(request.name());
        size.setDisplayOrder(request.displayOrder() != null ? request.displayOrder() : 0);
        size.setActive(request.active());
    }

    private SizeAdminResponse toResponse(Size s, Map<Long, String> names) {
        return new SizeAdminResponse(
                s.getId(),
                s.getName(),
                s.getDisplayOrder(),
                s.isActive(),
                names.get(s.getCreatedBy()),
                names.get(s.getUpdatedBy()),
                s.getCreatedAt(),
                s.getUpdatedAt());
    }
}
