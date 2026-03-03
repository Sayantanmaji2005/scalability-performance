package com.scalemart.api.dto;

import java.time.Instant;

public record AdminAuditLogResponse(
    Long id,
    String actorUsername,
    String targetUsername,
    String action,
    String details,
    Instant createdAt
) {
}
