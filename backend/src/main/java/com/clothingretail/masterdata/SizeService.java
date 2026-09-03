package com.clothingretail.masterdata;

import com.clothingretail.common.ConflictException;
import com.clothingretail.common.NotFoundException;
import com.clothingretail.masterdata.dto.SizeAdminRequest;
import com.clothingretail.masterdata.dto.SizeAdminResponse;
import com.clothingretail.masterdata.dto.SizeResponse;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class SizeService {

    private final SizeRepository repository;

    public SizeService(SizeRepository repository) {
        this.repository = repository;
    }

    public List<SizeAdminResponse> listAdmin() {
        return repository.findAll().stream().map(this::toResponse).toList();
    }

    public SizeAdminResponse getAdmin(Long id) {
        return toResponse(find(id));
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
        return toResponse(repository.save(size));
    }

    @Transactional
    public SizeAdminResponse update(Long id, SizeAdminRequest request) {
        Size size = find(id);
        apply(size, request);
        return toResponse(repository.save(size));
    }

    @Transactional
    public void delete(Long id) {
        repository.delete(find(id));
    }

    private Size find(Long id) {
        return repository.findById(id).orElseThrow(() -> new NotFoundException("Size not found: " + id));
    }

    private void apply(Size size, SizeAdminRequest request) {
        size.setName(request.name());
        size.setDisplayOrder(request.displayOrder() != null ? request.displayOrder() : 0);
        size.setActive(request.active());
    }

    private SizeAdminResponse toResponse(Size s) {
        return new SizeAdminResponse(s.getId(), s.getName(), s.getDisplayOrder(), s.isActive());
    }
}
