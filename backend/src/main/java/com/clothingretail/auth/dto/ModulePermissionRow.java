package com.clothingretail.auth.dto;

import com.clothingretail.auth.AdminModule;

public record ModulePermissionRow(AdminModule module, boolean canView, boolean canEdit) {}
