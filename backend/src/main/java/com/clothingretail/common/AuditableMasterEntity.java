package com.clothingretail.common;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.LastModifiedBy;

/**
 * Base for master-data entities (Category, Size, Color, Brand, Material, Vendor,
 * SubCategory, SizeGroup, DamageReason, ...) that need "created by / updated by"
 * attribution for the admin UI. Deliberately a plain {@code Long} rather than a
 * {@code @ManyToOne User} - this is the shape {@link org.springframework.data.domain.AuditorAware}
 * populates directly (see config.SecurityAuditorAware), and callers resolve the id to a
 * display name via {@link AuditorNameResolver} only when building a response DTO.
 * {@link BaseEntity}'s {@code @EntityListeners(AuditingEntityListener.class)} (inherited
 * from this class's superclass) is what actually triggers the population.
 */
@Getter
@Setter
@MappedSuperclass
public abstract class AuditableMasterEntity extends BaseEntity {

    @CreatedBy
    @Column(name = "created_by", updatable = false)
    private Long createdBy;

    @LastModifiedBy
    @Column(name = "updated_by")
    private Long updatedBy;
}
