package com.clothingretail.inventory;

import com.clothingretail.common.AuditorNameResolver;
import com.clothingretail.common.ConflictException;
import com.clothingretail.common.NotFoundException;
import com.clothingretail.inventory.dto.DamageReasonAdminRequest;
import com.clothingretail.inventory.dto.DamageReasonAdminResponse;
import com.clothingretail.inventory.dto.DamageReasonResponse;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional(readOnly = true)
public class DamageReasonService {

    private final DamageReasonRepository repository;
    private final DamageRecordRepository damageRecordRepository;
    private final AuditorNameResolver auditorNameResolver;

    public DamageReasonService(
            DamageReasonRepository repository, DamageRecordRepository damageRecordRepository, AuditorNameResolver auditorNameResolver) {
        this.repository = repository;
        this.damageRecordRepository = damageRecordRepository;
        this.auditorNameResolver = auditorNameResolver;
    }

    public List<DamageReasonAdminResponse> listAdmin(String q, Boolean active) {
        List<DamageReason> reasons = repository.findAll().stream()
                .filter(r -> matchesQuery(r, q))
                .filter(r -> active == null || r.isActive() == active)
                .toList();
        Map<Long, String> names = auditorNameResolver.resolveNames(
                reasons.stream().flatMap(r -> java.util.stream.Stream.of(r.getCreatedBy(), r.getUpdatedBy())).toList());
        return reasons.stream().map(r -> toResponse(r, names)).toList();
    }

    public DamageReasonAdminResponse getAdmin(Long id) {
        DamageReason reason = find(id);
        return toResponse(reason, auditorNameResolver.resolveNames(reason.getCreatedBy(), reason.getUpdatedBy()));
    }

    public List<DamageReasonResponse> listPublic() {
        return repository.findByActiveTrueOrderByDisplayOrderAscNameAsc().stream()
                .map(r -> new DamageReasonResponse(r.getId(), r.getName()))
                .toList();
    }

    @Transactional
    public DamageReasonAdminResponse create(DamageReasonAdminRequest request) {
        if (repository.existsByNameIgnoreCase(request.name())) {
            throw new ConflictException("A damage reason with this name already exists");
        }
        DamageReason reason = new DamageReason();
        apply(reason, request);
        DamageReason saved = repository.save(reason);
        return toResponse(saved, auditorNameResolver.resolveNames(saved.getCreatedBy(), saved.getUpdatedBy()));
    }

    @Transactional
    public DamageReasonAdminResponse update(Long id, DamageReasonAdminRequest request) {
        DamageReason reason = find(id);
        apply(reason, request);
        DamageReason saved = repository.save(reason);
        return toResponse(saved, auditorNameResolver.resolveNames(saved.getCreatedBy(), saved.getUpdatedBy()));
    }

    @Transactional
    public void delete(Long id) {
        DamageReason reason = find(id);
        long usageCount = damageRecordRepository.countByReasonId(id);
        if (usageCount > 0) {
            throw new ConflictException(
                    "This damage reason is currently used by " + usageCount + " damage record(s) and cannot be deleted. Deactivate it instead.");
        }
        repository.delete(reason);
    }

    private DamageReason find(Long id) {
        return repository.findById(id).orElseThrow(() -> new NotFoundException("Damage reason not found: " + id));
    }

    private boolean matchesQuery(DamageReason reason, String q) {
        return !StringUtils.hasText(q) || reason.getName().toLowerCase().contains(q.toLowerCase());
    }

    private void apply(DamageReason reason, DamageReasonAdminRequest request) {
        reason.setName(request.name());
        reason.setCode(request.code());
        reason.setDescription(request.description());
        reason.setDisplayOrder(request.displayOrder() != null ? request.displayOrder() : 0);
        reason.setActive(request.active());
    }

    private DamageReasonAdminResponse toResponse(DamageReason r, Map<Long, String> names) {
        return new DamageReasonAdminResponse(
                r.getId(),
                r.getName(),
                r.getCode(),
                r.getDescription(),
                r.getDisplayOrder(),
                r.isActive(),
                names.get(r.getCreatedBy()),
                names.get(r.getUpdatedBy()),
                r.getCreatedAt(),
                r.getUpdatedAt());
    }
}
