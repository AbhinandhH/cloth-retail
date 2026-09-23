package com.clothingretail.common;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import org.springframework.http.MediaType;

/**
 * Writes the same {@code ApiError} JSON shape {@link GlobalExceptionHandler} produces, but
 * directly to the raw response - for the rare cases that must reject a request before the
 * {@code DispatcherServlet} ever runs (a servlet filter), where {@code GlobalExceptionHandler}'s
 * {@code @RestControllerAdvice} can never be reached. Used by {@code SecurityConfig}'s own
 * 401/403 handlers and {@code SubscriptionAccessFilter}'s 402.
 */
public final class HttpErrorResponses {

    private HttpErrorResponses() {}

    public static void write(HttpServletResponse response, int status, String error, String message, String path)
            throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        String json = """
                {"timestamp":"%s","status":%d,"error":"%s","message":"%s","path":"%s"}""".formatted(
                Instant.now(), status, error, escapeJson(message), escapeJson(path));
        response.getWriter().write(json);
    }

    private static String escapeJson(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
