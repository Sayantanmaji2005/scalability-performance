package com.scalemart.api.dto;

import jakarta.validation.constraints.NotBlank;

public record VerifyEmailRequest(
    @NotBlank String username,
    @NotBlank String token
) {
}
