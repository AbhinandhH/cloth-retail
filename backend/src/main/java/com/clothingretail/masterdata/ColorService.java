package com.clothingretail.masterdata;

import com.clothingretail.common.ConflictException;
import com.clothingretail.common.NotFoundException;
import com.clothingretail.masterdata.dto.ColorAdminRequest;
import com.clothingretail.masterdata.dto.ColorAdminResponse;
import com.clothingretail.masterdata.dto.ColorResponse;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ColorService {

    private final ColorRepository repository;

    public ColorService(ColorRepository repository) {
        this.repository = repository;
    }

    public List<ColorAdminResponse> listAdmin() {
        return repository.findAll().stream().map(this::toResponse).toList();
    }

    public ColorAdminResponse getAdmin(Long id) {
        return toResponse(find(id));
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
        return toResponse(repository.save(color));
    }

    @Transactional
    public ColorAdminResponse update(Long id, ColorAdminRequest request) {
        Color color = find(id);
        apply(color, request);
        return toResponse(repository.save(color));
    }

    @Transactional
    public void delete(Long id) {
        repository.delete(find(id));
    }

    private Color find(Long id) {
        return repository.findById(id).orElseThrow(() -> new NotFoundException("Color not found: " + id));
    }

    private void apply(Color color, ColorAdminRequest request) {
        color.setName(request.name());
        color.setHexCode(request.hexCode());
        color.setDisplayOrder(request.displayOrder() != null ? request.displayOrder() : 0);
        color.setActive(request.active());
    }

    private ColorAdminResponse toResponse(Color c) {
        return new ColorAdminResponse(c.getId(), c.getName(), c.getHexCode(), c.getDisplayOrder(), c.isActive());
    }
}
