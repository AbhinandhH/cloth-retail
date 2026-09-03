package com.clothingretail.masterdata;

import com.clothingretail.common.ConflictException;
import com.clothingretail.common.NotFoundException;
import com.clothingretail.masterdata.dto.BrandAdminRequest;
import com.clothingretail.masterdata.dto.BrandAdminResponse;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class BrandService {

    private final BrandRepository repository;

    public BrandService(BrandRepository repository) {
        this.repository = repository;
    }

    public List<BrandAdminResponse> listAdmin() {
        return repository.findAll().stream().map(this::toResponse).toList();
    }

    public BrandAdminResponse getAdmin(Long id) {
        return toResponse(find(id));
    }

    @Transactional
    public BrandAdminResponse create(BrandAdminRequest request) {
        if (repository.existsByNameIgnoreCase(request.name())) {
            throw new ConflictException("A brand with this name already exists");
        }
        Brand brand = new Brand();
        apply(brand, request);
        return toResponse(repository.save(brand));
    }

    @Transactional
    public BrandAdminResponse update(Long id, BrandAdminRequest request) {
        Brand brand = find(id);
        apply(brand, request);
        return toResponse(repository.save(brand));
    }

    @Transactional
    public void delete(Long id) {
        repository.delete(find(id));
    }

    private Brand find(Long id) {
        return repository.findById(id).orElseThrow(() -> new NotFoundException("Brand not found: " + id));
    }

    private void apply(Brand brand, BrandAdminRequest request) {
        brand.setName(request.name());
        brand.setDisplayOrder(request.displayOrder() != null ? request.displayOrder() : 0);
        brand.setActive(request.active());
    }

    private BrandAdminResponse toResponse(Brand b) {
        return new BrandAdminResponse(b.getId(), b.getName(), b.getDisplayOrder(), b.isActive());
    }
}
