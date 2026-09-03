package com.clothingretail.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.List;

/**
 * Standard JSON error body returned for every 4xx/5xx response.
 * {@code fieldErrors} is omitted (not serialized) unless the failure was a
 * Bean Validation failure.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        List<FieldErrorItem> fieldErrors) {

    public record FieldErrorItem(String field, String message) {}

    public static ApiError of(int status, String error, String message, String path) {
        return new ApiError(Instant.now(), status, error, message, path, null);
    }

    public static ApiError of(int status, String error, String message, String path, List<FieldErrorItem> fieldErrors) {
        return new ApiError(Instant.now(), status, error, message, path, fieldErrors);
    }
}
