package com.clothingretail.activity.service;

import com.clothingretail.activity.ActivityLog;
import com.clothingretail.activity.dto.ActivityLogRow;
import com.clothingretail.activity.repository.ActivityLogRepository;
import com.clothingretail.activity.repository.ActivitySpecifications;
import com.clothingretail.common.AuditorNameResolver;
import java.time.Instant;
import java.util.Map;
import java.util.stream.Stream;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@Log4j2
public class ActivityLogServiceImpl implements ActivityLogService {

    private final ActivityLogRepository repository;
    private final AuditorNameResolver auditorNameResolver;

    public ActivityLogServiceImpl(ActivityLogRepository repository, AuditorNameResolver auditorNameResolver) {
        this.repository = repository;
        this.auditorNameResolver = auditorNameResolver;
    }

    /**
     * REQUIRES_NEW so a write that fails partway (e.g. a validation exception thrown after the
     * interceptor's afterCompletion already ran) can never roll this log entry back along with
     * it - the whole point of an activity log is to also capture failed/rejected attempts
     * faithfully, in their own independent transaction.
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(Long actorId, String httpMethod, String path, String module, int statusCode) {
        ActivityLog log = new ActivityLog();
        log.setActorId(actorId);
        log.setHttpMethod(httpMethod);
        log.setPath(path);
        log.setModule(module);
        log.setStatusCode(statusCode);
        repository.save(log);
    }

    @Override
    public Page<ActivityLogRow> list(
            Long actorId, String module, String httpMethod, Instant dateFrom, Instant dateTo, int page, int size) {
        log.info("[2100] Listing activity log actorId={} module={} httpMethod={} dateFrom={} dateTo={} page={} size={}",
                actorId, module, httpMethod, dateFrom, dateTo, page, size);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        var spec = ActivitySpecifications.filter(actorId, module, httpMethod, dateFrom, dateTo);
        Page<ActivityLog> result = repository.findAll(spec, pageable);
        Map<Long, String> names = auditorNameResolver.resolveNames(result.getContent().stream()
                .map(ActivityLog::getActorId)
                .flatMap(id -> id != null ? Stream.of(id) : Stream.empty())
                .toList());
        return result.map(entry -> new ActivityLogRow(
                entry.getId(),
                entry.getActorId(),
                names.get(entry.getActorId()),
                entry.getHttpMethod(),
                entry.getPath(),
                entry.getModule(),
                entry.getStatusCode(),
                entry.getCreatedAt()));
    }
}
