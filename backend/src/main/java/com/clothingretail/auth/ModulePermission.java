package com.clothingretail.auth;

import com.clothingretail.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * One row per (user, module): whether that ADMIN or EMPLOYEE account can view and/or edit a given
 * store-operational module (see AdminModule). Checked fresh from the DB on every request (see
 * ModulePermissionService, exposed to @PreAuthorize as the "modulePermission" bean) rather than
 * baked into the JWT - an access change must take effect immediately, not wait for the caller's
 * 15-minute access token to expire.
 */
@Entity
@Table(name = "module_permissions", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "module"}))
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class ModulePermission extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AdminModule module;

    @Column(name = "can_view", nullable = false)
    private boolean canView = false;

    @Column(name = "can_edit", nullable = false)
    private boolean canEdit = false;
}
