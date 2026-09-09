package com.clothingretail.auth;

import com.clothingretail.common.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Signup data held OUTSIDE {@link User}/{@code users} until every required OTP channel is
 * confirmed - see {@link AuthService#register} and {@link AuthService#verifyOtp}, the only place
 * this ever gets promoted into a real {@link User} + CustomerProfile. An abandoned signup that
 * never finishes verification leaves nothing behind in {@code users} - only a row here, reclaimed
 * eventually by {@link PendingRegistrationCleanupJob}.
 */
@Entity
@Table(name = "pending_registrations")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class PendingRegistration extends BaseEntity {

    @Column(name = "full_name", nullable = false, length = 150)
    private String fullName;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "mobile_number", length = 20)
    private String mobileNumber;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified = false;

    @Column(name = "mobile_verified", nullable = false)
    private boolean mobileVerified = false;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    // Modeled here (not just the DB-level ON DELETE CASCADE) so Hibernate knows to delete these
    // BEFORE the parent row at flush time - without this, deleting a PendingRegistration that
    // still has a managed OtpCode pointing at it throws TransientPropertyValueException at commit,
    // since Hibernate's own flush ordering has no way to know about a cascade it only exists at
    // the database level. Same pattern as Product/ProductColorMedia elsewhere in this codebase.
    @EqualsAndHashCode.Exclude
    @OneToMany(mappedBy = "pendingRegistration", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OtpCode> otpCodes = new ArrayList<>();

    public boolean hasMobile() {
        return mobileNumber != null && !mobileNumber.isBlank();
    }
}
