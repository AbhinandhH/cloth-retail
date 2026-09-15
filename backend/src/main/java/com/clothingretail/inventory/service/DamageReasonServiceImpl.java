package com.clothingretail.inventory.service;

import com.clothingretail.common.AuditorNameResolver;
import com.clothingretail.common.ConflictException;
import com.clothingretail.common.NotFoundException;
import com.clothingretail.inventory.DamageReason;
import com.clothingretail.inventory.dto.DamageReasonAdminRequest;
import com.clothingretail.inventory.dto.DamageReasonAdminResponse;
import com.clothingretail.inventory.dto.DamageReasonResponse;
import com.clothingretail.inventory.repository.DamageRecordRepository;
import com.clothingretail.inventory.repository.DamageReasonRepository;
import java.util.List;
import java.util.Map;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional(readOnly = true)
@Log4j2
public class DamageReasonServiceImpl implements DamageReasonService {

    private final DamageReasonRepository repository;
    private final DamageRecordRepository damageRecordRepository;
    private final AuditorNameResolver auditorNameResolver;

    public DamageReasonServiceImpl(
            DamageReasonRepository repository, DamageRecordRepository damageRecordRepository, AuditorNameResolver auditorNameResolver) {
        this.repository = repository;
        this.damageRecordRepository = damageRecordRepository;
        this.auditorNameResolver = auditorNameResolver;
    }

    @Override
    public List<DamageReasonAdminResponse> listAdmin(String q, Boolean active) {
        log.info("[1300] Listing damage reasons (admin) q={}, active={}", q, active);
        List<DamageReason> reasons = repository.findAll().stream()
                .filter(r -> matchesQuery(r, q))
                .filter(r -> active == null || r.isActive() == active)
                .toList();
        log.info("[1301] Found {} damage reason(s) for q={}, active={}", reasons.size(), q, active);
        Map<Long, String> names = auditorNameResolver.resolveNames(
                reasons.stream().flatMap(r -> java.util.stream.Stream.of(r.getCreatedBy(), r.getUpdatedBy())).toList());
        return reasons.stream().map(r -> toResponse(r, names)).toList();
    }

    @Override
    public DamageReasonAdminResponse getAdmin(Long id) {
        log.info("[1302] Fetching damage reason admin view id={}", id);
        DamageReason reason = find(id);
        return toResponse(reason, auditorNameResolver.resolveNames(reason.getCreatedBy(), reason.getUpdatedBy()));
    }

    @Override
    public List<DamageReasonResponse> listPublic() {
        log.info("[1304] Listing active public damage reasons");
        List<DamageReasonResponse> result = repository.findByActiveTrueOrderByDisplayOrderAscNameAsc().stream()
                .map(r -> new DamageReasonResponse(r.getId(), r.getName()))
                .toList();
        log.info("[1305] Found {} active public damage reason(s)", result.size());
        return result;
    }

    @Override
    @Transactional
    public DamageReasonAdminResponse create(DamageReasonAdminRequest request) {
        log.info("[1306] Creating damage reason name={}, code={}", request.name(), request.code());
        if (repository.existsByNameIgnoreCase(request.name())) {
            log.error("[1307] Damage reason creation conflict - name already exists: {}", request.name());
            throw new ConflictException("A damage reason with this name already exists");
        }
        DamageReason reason = new DamageReason();
        apply(reason, request);
        DamageReason saved = repository.save(reason);
        log.info("[1308] Damage reason created id={}, name={}", saved.getId(), saved.getName());
        return toResponse(saved, auditorNameResolver.resolveNames(saved.getCreatedBy(), saved.getUpdatedBy()));
    }

    @Override
    @Transactional
    public DamageReasonAdminResponse update(Long id, DamageReasonAdminRequest request) {
        log.info("[1309] Updating damage reason id={}, name={}, code={}", id, request.name(), request.code());
        DamageReason reason = find(id);
        apply(reason, request);
        DamageReason saved = repository.save(reason);
        log.info("[1310] Damage reason updated id={}, name={}", saved.getId(), saved.getName());
        return toResponse(saved, auditorNameResolver.resolveNames(saved.getCreatedBy(), saved.getUpdatedBy()));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        log.info("[1311] Deleting damage reason id={}", id);
        DamageReason reason = find(id);
        long usageCount = damageRecordRepository.countByReasonId(id);
        if (usageCount > 0) {
            log.error("[1312] Cannot delete damage reason id={} - in use by {} damage record(s)", id, usageCount);
            throw new ConflictException(
                    "This damage reason is currently used by " + usageCount + " damage record(s) and cannot be deleted. Deactivate it instead.");
        }
        repository.delete(reason);
        log.info("[1313] Damage reason deleted id={}", id);
    }

    private DamageReason find(Long id) {
        return repository.findById(id).orElseThrow(() -> {
            log.error("[1303] Damage reason not found id={}", id);
            return new NotFoundException("Damage reason not found: " + id);
        });
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
