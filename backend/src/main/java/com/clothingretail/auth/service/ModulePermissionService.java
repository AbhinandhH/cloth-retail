package com.clothingretail.auth.service;

import com.clothingretail.auth.AdminModule;
import com.clothingretail.auth.dto.ModulePermissionRow;
import java.util.List;

/**
 * Exposed to {@code @PreAuthorize} SpEL expressions as the "modulePermission" bean (see
 * ModulePermissionServiceImpl's {@code @Component} name) - e.g.
 * {@code @PreAuthorize("hasRole('ADMIN') or (hasRole('EMPLOYEE') and @modulePermission.hasAccess('ORDERS'))")}.
 * {@link #hasAccess} resolves view-vs-edit from the current request's HTTP method (GET = view,
 * everything else = edit) so every one of the 8 operational controllers only needs a single
 * class-level annotation, not one per method.
 */
public interface ModulePermissionService {

    /** True if the caller (resolved from the security context) may perform the current request against this module. */
    boolean hasAccess(String module);

    boolean hasView(Long userId, AdminModule module);

    boolean hasEdit(Long userId, AdminModule module);

    /** Every module's current grant for this user, one row per AdminModule value (defaults to view=false, edit=false if never granted). */
    List<ModulePermissionRow> listForUser(Long userId);

    /** Replaces this user's entire permission set with the given grants - modules omitted from `grants` are set to no access. */
    void replaceForUser(Long userId, List<ModulePermissionRow> grants);

    /** Seeds full view+edit access on every module - used when a new ADMIN account is created. */
    void grantAllModules(Long userId);
}
