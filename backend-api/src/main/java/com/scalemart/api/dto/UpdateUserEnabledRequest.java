package com.scalemart.api.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateUserEnabledRequest(
    @NotNull Boolean enabled
) {
}
