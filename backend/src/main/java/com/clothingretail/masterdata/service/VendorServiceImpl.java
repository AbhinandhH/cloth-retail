package com.clothingretail.masterdata.service;

import com.clothingretail.common.AuditorNameResolver;
import com.clothingretail.common.ConflictException;
import com.clothingretail.common.NotFoundException;
import com.clothingretail.inventory.PurchaseRepository;
import com.clothingretail.masterdata.Vendor;
import com.clothingretail.masterdata.dto.VendorAdminRequest;
import com.clothingretail.masterdata.dto.VendorAdminResponse;
import com.clothingretail.masterdata.dto.VendorResponse;
import com.clothingretail.masterdata.repository.VendorRepository;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional(readOnly = true)
@Log4j2
public class VendorServiceImpl implements VendorService {

    private final VendorRepository repository;
    private final PurchaseRepository purchaseRepository;
    private final AuditorNameResolver auditorNameResolver;

    public VendorServiceImpl(VendorRepository repository, PurchaseRepository purchaseRepository, AuditorNameResolver auditorNameResolver) {
        this.repository = repository;
        this.purchaseRepository = purchaseRepository;
        this.auditorNameResolver = auditorNameResolver;
    }

    @Override
    public List<VendorAdminResponse> listAdmin(String q, Boolean active) {
        log.info("[1467] Listing vendors query={} active={}", q, active);
        List<Vendor> vendors = repository.findAll().stream()
                .filter(v -> matchesQuery(v, q))
                .filter(v -> active == null || v.isActive() == active)
                .toList();
        Map<Long, String> names = auditorNameResolver.resolveNames(
                vendors.stream().flatMap(v -> Stream.of(v.getCreatedBy(), v.getUpdatedBy())).toList());
        return vendors.stream().map(v -> toResponse(v, names)).toList();
    }

    @Override
    public VendorAdminResponse getAdmin(Long id) {
        log.info("[1468] Fetching vendor id={}", id);
        Vendor vendor = find(id);
        return toResponse(vendor, auditorNameResolver.resolveNames(vendor.getCreatedBy(), vendor.getUpdatedBy()));
    }

    @Override
    public List<VendorResponse> listPublic() {
        log.info("[1469] Listing public vendors");
        return repository.findByActiveTrueOrderByNameAsc().stream()
                .map(v -> new VendorResponse(v.getId(), v.getName()))
                .toList();
    }

    @Override
    @Transactional
    public VendorAdminResponse create(VendorAdminRequest request) {
        log.info("[1470] Creating vendor name={} contactName={} contactEmail={} contactPhone={} active={}",
                request.name(), request.contactName(), request.contactEmail(), request.contactPhone(), request.active());
        if (repository.existsByNameIgnoreCase(request.name())) {
            log.error("[1471] Cannot create vendor - name={} already exists", request.name());
            throw new ConflictException("A vendor with this name already exists");
        }
        if (StringUtils.hasText(request.contactEmail()) && repository.existsByContactEmailIgnoreCase(request.contactEmail())) {
            log.error("[1476] Cannot create vendor - contactEmail={} already exists", request.contactEmail());
            throw new ConflictException("A vendor with this email already exists");
        }
        if (StringUtils.hasText(request.contactPhone()) && repository.existsByContactPhone(request.contactPhone())) {
            log.error("[1477] Cannot create vendor - contactPhone={} already exists", request.contactPhone());
            throw new ConflictException("A vendor with this phone number already exists");
        }
        Vendor vendor = new Vendor();
        apply(vendor, request);
        Vendor saved = repository.save(vendor);
        return toResponse(saved, auditorNameResolver.resolveNames(saved.getCreatedBy(), saved.getUpdatedBy()));
    }

    @Override
    @Transactional
    public VendorAdminResponse update(Long id, VendorAdminRequest request) {
        log.info("[1472] Updating vendor id={} name={} contactEmail={} contactPhone={} active={}",
                id, request.name(), request.contactEmail(), request.contactPhone(), request.active());
        Vendor vendor = find(id);
        if (repository.existsByNameIgnoreCaseAndIdNot(request.name(), id)) {
            log.error("[1478] Cannot update vendor id={} - name={} already used by another vendor", id, request.name());
            throw new ConflictException("A vendor with this name already exists");
        }
        if (StringUtils.hasText(request.contactEmail()) && repository.existsByContactEmailIgnoreCaseAndIdNot(request.contactEmail(), id)) {
            log.error("[1479] Cannot update vendor id={} - contactEmail={} already used by another vendor", id, request.contactEmail());
            throw new ConflictException("A vendor with this email already exists");
        }
        if (StringUtils.hasText(request.contactPhone()) && repository.existsByContactPhoneAndIdNot(request.contactPhone(), id)) {
            log.error("[1480] Cannot update vendor id={} - contactPhone={} already used by another vendor", id, request.contactPhone());
            throw new ConflictException("A vendor with this phone number already exists");
        }
        apply(vendor, request);
        Vendor saved = repository.save(vendor);
        return toResponse(saved, auditorNameResolver.resolveNames(saved.getCreatedBy(), saved.getUpdatedBy()));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        log.info("[1473] Deleting vendor id={}", id);
        Vendor vendor = find(id);
        long usageCount = purchaseRepository.countByVendorId(id);
        if (usageCount > 0) {
            log.error("[1474] Cannot delete vendor id={} - {} purchases depend on it", id, usageCount);
            throw new ConflictException(
                    "This vendor is currently used by " + usageCount + " purchases and cannot be deleted. Deactivate it instead.");
        }
        repository.delete(vendor);
    }

    private Vendor find(Long id) {
        return repository.findById(id).orElseThrow(() -> {
            log.error("[1475] Vendor not found id={}", id);
            return new NotFoundException("Vendor not found: " + id);
        });
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
