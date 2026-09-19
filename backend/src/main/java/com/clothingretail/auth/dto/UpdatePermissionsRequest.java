package com.clothingretail.auth.dto;

import jakarta.validation.constraints.NotNull;
import java.util.List;

/** Modules omitted from `grants` are set to no access - this always replaces the full set, never a partial patch. */
public record UpdatePermissionsRequest(@NotNull(message = "must not be null") List<ModulePermissionRow> grants) {}
