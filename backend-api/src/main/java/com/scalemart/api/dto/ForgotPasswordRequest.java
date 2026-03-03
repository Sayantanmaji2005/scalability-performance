package com.scalemart.api.dto;

import jakarta.validation.constraints.NotBlank;

public record ForgotPasswordRequest(
    @NotBlank String username
) {
}
