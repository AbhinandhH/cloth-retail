package com.clothingretail.auth;

/**
 * The fixed set of roles. SUPER_ADMIN is the software owner - it has every privilege ADMIN has
 * (see SecurityConfig's RoleHierarchy bean: ROLE_SUPER_ADMIN > ROLE_ADMIN), on top of its own
 * software-owner-only powers (creating a store's first ADMIN account, subscription billing).
 * ADMIN is the store owner - full store access, can create further ADMIN/EMPLOYEE accounts and
 * grant each one per-module view/edit access (see ModulePermission). EMPLOYEE is store staff,
 * restricted to whatever an ADMIN has granted them, and does NOT inherit from ADMIN. CUSTOMER is
 * unchanged.
 */
public enum RoleName {
    SUPER_ADMIN,
    ADMIN,
    EMPLOYEE,
    CUSTOMER
}
