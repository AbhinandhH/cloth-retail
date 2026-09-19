package com.clothingretail.auth;

/**
 * The 8 store-operational admin modules an ADMIN can grant an EMPLOYEE (or another ADMIN)
 * view/edit access to, one-to-one with {@code AdminHome.tsx}'s operational module tiles. Deliberately
 * excludes the governance-only screens (Tax Settings, Site Configuration, Themes, Notification/SMTP
 * settings, Staff management) - those stay ADMIN-only regardless of any permission grant, since
 * they're store-governance decisions, not day-to-day operational tasks. See ModulePermission.
 */
public enum AdminModule {
    DASHBOARD,
    PRODUCTS,
    INVENTORY,
    ORDERS,
    RETURNS,
    CUSTOMERS,
    MASTERS,
    REPORTS
}
