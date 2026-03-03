package com.scalemart.api.controller;

import com.scalemart.api.dto.AdminAuditLogResponse;
import com.scalemart.api.service.AdminAuditService;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/audit-logs")
public class AdminAuditController {

    private final AdminAuditService adminAuditService;

    public AdminAuditController(AdminAuditService adminAuditService) {
        this.adminAuditService = adminAuditService;
    }

    @GetMapping
    public Page<AdminAuditLogResponse> listLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size,
            @RequestParam(required = false) String actor,
            @RequestParam(required = false) String target,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {
        return adminAuditService.search(
            actor,
            target,
            action,
            parseInstantOrNull(from),
            parseInstantOrNull(to),
            page,
            size
        );
    }

    @GetMapping(value = "/export", produces = "text/csv")
    public ResponseEntity<String> exportLogs(
            @RequestParam(defaultValue = "500") int limit,
            @RequestParam(required = false) String actor,
            @RequestParam(required = false) String target,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {
        String csv = adminAuditService.exportCsv(
            actor,
            target,
            action,
            parseInstantOrNull(from),
            parseInstantOrNull(to),
            limit
        );
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=admin-audit-logs.csv")
            .contentType(MediaType.parseMediaType("text/csv"))
            .body(csv);
    }

    private Instant parseInstantOrNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        String normalized = value.trim();
        try {
            return Instant.parse(normalized);
        } catch (DateTimeParseException ignored) {
            try {
                LocalDateTime localDateTime = LocalDateTime.parse(normalized);
                return localDateTime.atZone(ZoneId.systemDefault()).toInstant();
            } catch (DateTimeParseException exception) {
                throw new IllegalArgumentException("Invalid datetime format: " + value);
            }
        }
    }
}
