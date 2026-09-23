package com.clothingretail.activity.controller;

import com.clothingretail.activity.dto.ActivityLogRow;
import com.clothingretail.activity.service.ActivityLogService;
import com.clothingretail.common.PageResponse;
import java.time.Instant;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Read-only, ADMIN-only (governance tier, like Staff) - no write/delete endpoint, the log is immutable. */
@RestController
@RequestMapping("/api/admin/activity-log")
@PreAuthorize("hasRole('ADMIN')")
public class AdminActivityLogController {

    private final ActivityLogService activityLogService;

    public AdminActivityLogController(ActivityLogService activityLogService) {
        this.activityLogService = activityLogService;
    }

    @GetMapping
    public PageResponse<ActivityLogRow> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long actorId,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String httpMethod,
            @RequestParam(required = false) Instant dateFrom,
            @RequestParam(required = false) Instant dateTo) {
        return PageResponse.of(activityLogService.list(actorId, module, httpMethod, dateFrom, dateTo, page, size));
    }
}
