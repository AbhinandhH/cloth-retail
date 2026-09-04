package com.clothingretail.payment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record SimulatePaymentRequest(
        @NotBlank(message = "must not be blank") String gatewayReference,
        @NotBlank(message = "must not be blank")
                @Pattern(regexp = "SUCCESS|FAILURE", message = "must be SUCCESS or FAILURE")
                String outcome) {}
