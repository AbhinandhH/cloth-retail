package com.clothingretail.notification.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * {@code password} is optional: null/blank means "keep the currently stored password" (so
 * changing, say, the from-address doesn't force re-entering it) - only a non-blank value replaces
 * it. See SmtpSettingsService.update().
 */
public record SmtpSettingsUpdateRequest(
        String host,
        @NotNull(message = "must not be null") @Min(value = 1, message = "must be between 1 and 65535")
                @Max(value = 65535, message = "must be between 1 and 65535") Integer port,
        String username,
        String password,
        String fromAddress,
        @NotNull(message = "must not be null") Boolean useStarttls) {}
