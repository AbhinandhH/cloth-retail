package com.clothingretail.activity;

import com.clothingretail.activity.service.ActivityLogService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Set;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Writes one {@link ActivityLog} row per completed admin write request - registered for
 * /api/admin/** by {@link ActivityLogWebConfig}. Only POST/PUT/PATCH/DELETE are logged (a GET is
 * a read, not a change); {@code afterCompletion} (not {@code preHandle}) so the actual response
 * status is known and captured, including for a request that ended in a validation error or an
 * access-denied rejection.
 */
public class ActivityLoggingInterceptor implements HandlerInterceptor {

    private static final Set<String> LOGGED_METHODS = Set.of("POST", "PUT", "PATCH", "DELETE");

    private final ActivityLogService activityLogService;

    public ActivityLoggingInterceptor(ActivityLogService activityLogService) {
        this.activityLogService = activityLogService;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        String method = request.getMethod();
        if (!LOGGED_METHODS.contains(method)) {
            return;
        }
        String path = request.getRequestURI();
        // Admin login/logout are auth events, not admin-console changes - and aren't meaningfully
        // attributable to an actor yet at login time anyway.
        if (path.startsWith("/api/admin/auth/")) {
            return;
        }
        String module = deriveModule(path);
        Long actorId = currentUserId();
        activityLogService.record(actorId, method, path, module, response.getStatus());
    }

    /** The path segment right after /api/admin/ - e.g. "/api/admin/products/5" -> "products". */
    private String deriveModule(String path) {
        String prefix = "/api/admin/";
        if (!path.startsWith(prefix)) {
            return "other";
        }
        String rest = path.substring(prefix.length());
        int slash = rest.indexOf('/');
        return slash >= 0 ? rest.substring(0, slash) : rest;
    }

    private Long currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Long userId)) {
            return null;
        }
        return userId;
    }
}
