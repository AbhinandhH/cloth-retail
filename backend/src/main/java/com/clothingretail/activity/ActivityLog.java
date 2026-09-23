package com.clothingretail.activity;

import com.clothingretail.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * One row per admin-console write request (POST/PUT/PATCH/DELETE under /api/admin/**), written
 * automatically by {@link ActivityLoggingInterceptor} - not a per-service-method audit trail like
 * {@code OrderStatusHistory}/{@code InventoryTransaction}, which capture domain-specific detail
 * (what changed, not just that something did). This is the general-purpose trail across
 * everything else (masters, staff, site config, tax, theme, ...) that has no dedicated history
 * table of its own.
 *
 * Deliberately never stores the request or response body - some admin write bodies carry
 * passwords (staff creation, SMTP settings), so only method/path/status/actor/time are captured.
 * Immutable once written - no update/delete path exists.
 */
@Entity
@Table(name = "activity_log")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class ActivityLog extends BaseEntity {

    /** Null means the request reached this filter with no resolved authentication (shouldn't happen for an authenticated-only path, but never assumed). */
    @Column(name = "actor_id")
    private Long actorId;

    @Column(name = "http_method", nullable = false, length = 10)
    private String httpMethod;

    @Column(nullable = false, length = 255)
    private String path;

    /** The path segment right after /api/admin/ (e.g. "products", "staff") - a coarse grouping, not a validated enum. */
    @Column(nullable = false, length = 50)
    private String module;

    @Column(name = "status_code", nullable = false)
    private int statusCode;
}
