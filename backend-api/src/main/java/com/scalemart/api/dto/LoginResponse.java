package com.scalemart.api.dto;

public record LoginResponse(
    String token,
    String refreshToken,
    long expiresAtEpochSeconds,
    String role
) {
}
