package com.clothingretail.masterdata;

import com.clothingretail.common.AuditorNameResolver;
import com.clothingretail.common.ConflictException;
import com.clothingretail.common.NotFoundException;
import com.clothingretail.inventory.PurchaseRepository;
import com.clothingretail.masterdata.dto.VendorAdminRequest;
import com.clothingretail.masterdata.dto.VendorAdminResponse;
import com.clothingretail.masterdata.dto.VendorResponse;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional(readOnly = true)
public class VendorService {

    private final VendorRepository repository;
    private final PurchaseRepository purchaseRepository;
    private final AuditorNameResolver auditorNameResolver;

    public VendorService(VendorRepository repository, PurchaseRepository purchaseRepository, AuditorNameResolver auditorNameResolver) {
        this.repository = repository;
        this.purchaseRepository = purchaseRepository;
        this.auditorNameResolver = auditorNameResolver;
    }

    public List<VendorAdminResponse> listAdmin(String q, Boolean active) {
        List<Vendor> vendors = repository.findAll().stream()
                .filter(v -> matchesQuery(v, q))
                .filter(v -> active == null || v.isActive() == active)
                .toList();
        Map<Long, String> names = auditorNameResolver.resolveNames(
                vendors.stream().flatMap(v -> Stream.of(v.getCreatedBy(), v.getUpdatedBy())).toList());
        return vendors.stream().map(v -> toResponse(v, names)).toList();
    }

    public VendorAdminResponse getAdmin(Long id) {
        Vendor vendor = find(id);
        return toResponse(vendor, auditorNameResolver.resolveNames(vendor.getCreatedBy(), vendor.getUpdatedBy()));
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
        Vendor saved = repository.save(vendor);
        return toResponse(saved, auditorNameResolver.resolveNames(saved.getCreatedBy(), saved.getUpdatedBy()));
    }

    @Transactional
    public VendorAdminResponse update(Long id, VendorAdminRequest request) {
        Vendor vendor = find(id);
        apply(vendor, request);
        Vendor saved = repository.save(vendor);
        return toResponse(saved, auditorNameResolver.resolveNames(saved.getCreatedBy(), saved.getUpdatedBy()));
    }

    @Transactional
    public void delete(Long id) {
        Vendor vendor = find(id);
        long usageCount = purchaseRepository.countByVendorId(id);
        if (usageCount > 0) {
            throw new ConflictException(
                    "This vendor is currently used by " + usageCount + " purchases and cannot be deleted. Deactivate it instead.");
        }
        repository.delete(vendor);
    }

    private Vendor find(Long id) {
        return repository.findById(id).orElseThrow(() -> new NotFoundException("Vendor not found: " + id));
    }

    private boolean matchesQuery(Vendor vendor, String q) {
        return !StringUtils.hasText(q) || vendor.getName().toLowerCase().contains(q.toLowerCase());
    }

    private void apply(Vendor vendor, VendorAdminRequest request) {
        vendor.setName(request.name());
        vendor.setContactName(request.contactName());
        vendor.setContactEmail(request.contactEmail());
        vendor.setContactPhone(request.contactPhone());
        vendor.setActive(request.active());
    }

    private VendorAdminResponse toResponse(Vendor v, Map<Long, String> names) {
        return new VendorAdminResponse(
                v.getId(),
                v.getName(),
                v.getContactName(),
                v.getContactEmail(),
                v.getContactPhone(),
                v.isActive(),
                names.get(v.getCreatedBy()),
                names.get(v.getUpdatedBy()),
                v.getCreatedAt(),
                v.getUpdatedAt());
    }
}
