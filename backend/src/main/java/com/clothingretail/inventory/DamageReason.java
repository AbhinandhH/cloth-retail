package com.clothingretail.inventory;

import com.clothingretail.common.AuditableMasterEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Why a variant's stock was marked damaged. Was a plain enum until this master-data
 * pass; converted to a real entity (unlike ProductStatus/StockStatus/InventoryTransactionType/
 * RoleName, which drive real code branching and stay enums) so admins can add/rename/deactivate
 * reasons without a deploy. See V6__master_data_management.sql for the 1:1 seed from the old
 * enum constants (matched via {@code code}).
 */
@Entity
@Table(name = "damage_reasons")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class DamageReason extends AuditableMasterEntity {

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 30)
    private String code;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "display_order", nullable = false)
    private int displayOrder = 0;

    @Column(nullable = false)
    private boolean active = true;
}
