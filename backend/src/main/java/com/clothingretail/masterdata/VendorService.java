package com.clothingretail.masterdata;

import com.clothingretail.common.ConflictException;
import com.clothingretail.common.NotFoundException;
import com.clothingretail.masterdata.dto.VendorAdminRequest;
import com.clothingretail.masterdata.dto.VendorAdminResponse;
import com.clothingretail.masterdata.dto.VendorResponse;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class VendorService {

    private final VendorRepository repository;

    public VendorService(VendorRepository repository) {
        this.repository = repository;
    }

    public List<VendorAdminResponse> listAdmin() {
        return repository.findAll().stream().map(this::toResponse).toList();
    }

    public VendorAdminResponse getAdmin(Long id) {
        return toResponse(find(id));
    }

    public List<VendorResponse> listPublic() {
        return repository.findByActiveTrueOrderByNameAsc().stream()
                .map(v -> new VendorResponse(v.getId(), v.getName()))
                .toList();
    }

    @Transactional
    public VendorAdminResponse create(VendorAdminRequest request) {
        if (repository.existsByNameIgnoreCase(request.name())) {
            throw new ConflictException("A vendor with this name already exists");
        }
        Vendor vendor = new Vendor();
        apply(vendor, request);
        return toResponse(repository.save(vendor));
    }

    @Transactional
    public VendorAdminResponse update(Long id, VendorAdminRequest request) {
        Vendor vendor = find(id);
        apply(vendor, request);
        return toResponse(repository.save(vendor));
    }

    @Transactional
    public void delete(Long id) {
        repository.delete(find(id));
    }

    private Vendor find(Long id) {
        return repository.findById(id).orElseThrow(() -> new NotFoundException("Vendor not found: " + id));
    }

    private void apply(Vendor vendor, VendorAdminRequest request) {
        vendor.setName(request.name());
        vendor.setContactName(request.contactName());
        vendor.setContactEmail(request.contactEmail());
        vendor.setContactPhone(request.contactPhone());
        vendor.setActive(request.active());
    }

    private VendorAdminResponse toResponse(Vendor v) {
        return new VendorAdminResponse(v.getId(), v.getName(), v.getContactName(), v.getContactEmail(), v.getContactPhone(), v.isActive());
    }
}
