package com.clothingretail.auth;

import com.clothingretail.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Refresh tokens are stored as a SHA-256 hash, never in plaintext - see
 * JwtService#hashToken. {@code revoked} is flipped on logout and on rotation
 * (old token revoked, new one issued) so a stolen, already-used refresh
 * token can't be replayed.
 */
@Entity
@Table(name = "refresh_tokens")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class RefreshToken extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "token_hash", nullable = false, unique = true, length = 255)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private boolean revoked = false;

    /**
     * When this token was last used to mint a new access/refresh pair (set to creation time when
     * first issued, updated on every rotation - see AuthServiceImpl#persistRefreshToken). Drives
     * the admin-configurable idle-timeout check in AuthServiceImpl#refresh: a refresh presented
     * after SiteConfiguration#idleTimeoutMinutes have elapsed since this timestamp is rejected
     * instead of rotated, even though the token itself hasn't hit its own expiresAt yet.
     */
    @Column(name = "last_used_at", nullable = false)
    private Instant lastUsedAt;

    public boolean isActive() {
        return !revoked && expiresAt.isAfter(Instant.now());
    }
}
