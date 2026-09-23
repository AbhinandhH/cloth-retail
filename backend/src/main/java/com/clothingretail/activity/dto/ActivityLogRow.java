package com.clothingretail.activity.dto;

import java.time.Instant;

public record ActivityLogRow(
        Long id, Long actorId, String actorName, String httpMethod, String path, String module, int statusCode, Instant createdAt) {}
