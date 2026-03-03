package com.scalemart.api.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateUserRoleRequest(
    @NotBlank String role
) {
}
