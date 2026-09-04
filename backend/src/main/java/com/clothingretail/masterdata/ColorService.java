package com.clothingretail.masterdata;

import com.clothingretail.common.AuditorNameResolver;
import com.clothingretail.common.ConflictException;
import com.clothingretail.common.NotFoundException;
import com.clothingretail.masterdata.dto.ColorAdminRequest;
import com.clothingretail.masterdata.dto.ColorAdminResponse;
import com.clothingretail.masterdata.dto.ColorResponse;
import com.clothingretail.product.ProductVariantRepository;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional(readOnly = true)
public class ColorService {

    private final ColorRepository repository;
    private final ProductVariantRepository productVariantRepository;
    private final AuditorNameResolver auditorNameResolver;

    public ColorService(
            ColorRepository repository, ProductVariantRepository productVariantRepository, AuditorNameResolver auditorNameResolver) {
        this.repository = repository;
        this.productVariantRepository = productVariantRepository;
        this.auditorNameResolver = auditorNameResolver;
    }

    public List<ColorAdminResponse> listAdmin(String q, Boolean active) {
        List<Color> colors = repository.findAll().stream()
                .filter(c -> matchesQuery(c, q))
                .filter(c -> active == null || c.isActive() == active)
                .toList();
        Map<Long, String> names = auditorNameResolver.resolveNames(
                colors.stream().flatMap(c -> Stream.of(c.getCreatedBy(), c.getUpdatedBy())).toList());
        return colors.stream().map(c -> toResponse(c, names)).toList();
    }

    public ColorAdminResponse getAdmin(Long id) {
        Color color = find(id);
        return toResponse(color, auditorNameResolver.resolveNames(color.getCreatedBy(), color.getUpdatedBy()));
    }

    public List<ColorResponse> listPublic() {
        return repository.findByActiveTrueOrderByDisplayOrderAscNameAsc().stream()
                .map(c -> new ColorResponse(c.getId(), c.getName(), c.getHexCode()))
                .toList();
    }

    @Transactional
    public ColorAdminResponse create(ColorAdminRequest request) {
        if (repository.existsByNameIgnoreCase(request.name())) {
            throw new ConflictException("A color with this name already exists");
        }
        Color color = new Color();
        apply(color, request);
        Color saved = repository.save(color);
        return toResponse(saved, auditorNameResolver.resolveNames(saved.getCreatedBy(), saved.getUpdatedBy()));
    }

    @Transactional
    public ColorAdminResponse update(Long id, ColorAdminRequest request) {
        Color color = find(id);
        apply(color, request);
        Color saved = repository.save(color);
        return toResponse(saved, auditorNameResolver.resolveNames(saved.getCreatedBy(), saved.getUpdatedBy()));
    }

    @Transactional
    public void delete(Long id) {
        Color color = find(id);
        long usageCount = productVariantRepository.countByColorId(id);
        if (usageCount > 0) {
            throw new ConflictException(
                    "This color is currently used by " + usageCount + " product variants and cannot be deleted. Deactivate it instead.");
        }
        repository.delete(color);
    }

    private Color find(Long id) {
        return repository.findById(id).orElseThrow(() -> new NotFoundException("Color not found: " + id));
    }

    private boolean matchesQuery(Color color, String q) {
        return !StringUtils.hasText(q) || color.getName().toLowerCase().contains(q.toLowerCase());
    }

    private void apply(Color color, ColorAdminRequest request) {
        color.setName(request.name());
        color.setHexCode(request.hexCode());
        color.setDisplayOrder(request.displayOrder() != null ? request.displayOrder() : 0);
        color.setActive(request.active());
    }

    private ColorAdminResponse toResponse(Color c, Map<Long, String> names) {
        return new ColorAdminResponse(
                c.getId(),
                c.getName(),
                c.getHexCode(),
                c.getDisplayOrder(),
                c.isActive(),
                names.get(c.getCreatedBy()),
                names.get(c.getUpdatedBy()),
                c.getCreatedAt(),
                c.getUpdatedAt());
    }
}
