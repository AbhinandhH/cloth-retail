package com.clothingretail.activity.service;

import com.clothingretail.activity.dto.ActivityLogRow;
import java.time.Instant;
import org.springframework.data.domain.Page;

public interface ActivityLogService {

    /** Called by {@link com.clothingretail.activity.ActivityLoggingInterceptor} after every admin write request completes. */
    void record(Long actorId, String httpMethod, String path, String module, int statusCode);

    Page<ActivityLogRow> list(Long actorId, String module, String httpMethod, Instant dateFrom, Instant dateTo, int page, int size);
}
