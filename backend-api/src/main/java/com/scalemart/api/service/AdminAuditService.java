package com.scalemart.api.service;

import com.scalemart.api.domain.AdminAuditLog;
import com.scalemart.api.dto.AdminAuditLogResponse;
import com.scalemart.api.repository.AdminAuditLogRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

@Service
public class AdminAuditService {

    private static final int MAX_LIMIT = 200;
    private static final int MAX_EXPORT_LIMIT = 5000;

    private final AdminAuditLogRepository adminAuditLogRepository;

    public AdminAuditService(AdminAuditLogRepository adminAuditLogRepository) {
        this.adminAuditLogRepository = adminAuditLogRepository;
    }

    public void record(String actorUsername, String action, String targetUsername, String details) {
        AdminAuditLog log = new AdminAuditLog();
        log.setActorUsername(actorUsername);
        log.setAction(action);
        log.setTargetUsername(targetUsername);
        log.setDetails(details == null ? "" : details);
        adminAuditLogRepository.save(log);
    }

    public Page<AdminAuditLogResponse> search(
        String actor,
        String target,
        String action,
        Instant from,
        Instant to,
        int page,
        int size) {
        validateDateRange(from, to);
        int safePage = Math.max(0, page);
        int safeSize = Math.max(1, Math.min(size, MAX_LIMIT));
        PageRequest pageRequest = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));

        Specification<AdminAuditLog> specification = Specification.where(null);
        if (hasText(actor)) {
            String actorValue = "%" + actor.trim().toLowerCase() + "%";
            specification = specification.and((root, query, cb) ->
                cb.like(cb.lower(root.get("actorUsername")), actorValue));
        }
        if (hasText(target)) {
            String targetValue = "%" + target.trim().toLowerCase() + "%";
            specification = specification.and((root, query, cb) ->
                cb.like(cb.lower(root.get("targetUsername")), targetValue));
        }
        if (hasText(action)) {
            String actionValue = action.trim().toUpperCase();
            specification = specification.and((root, query, cb) ->
                cb.equal(root.get("action"), actionValue));
        }
        if (from != null) {
            specification = specification.and((root, query, cb) ->
                cb.greaterThanOrEqualTo(root.get("createdAt"), from));
        }
        if (to != null) {
            specification = specification.and((root, query, cb) ->
                cb.lessThanOrEqualTo(root.get("createdAt"), to));
        }

        return adminAuditLogRepository.findAll(specification, pageRequest)
            .map(this::toResponse);
    }

    public String exportCsv(
        String actor,
        String target,
        String action,
        Instant from,
        Instant to,
        int limit) {
        validateDateRange(from, to);
        int safeLimit = Math.max(1, Math.min(limit, MAX_EXPORT_LIMIT));
        Page<AdminAuditLogResponse> page = search(actor, target, action, from, to, 0, safeLimit);
        List<String> lines = new ArrayList<>();
        lines.add("id,createdAt,action,actorUsername,targetUsername,details");
        for (AdminAuditLogResponse row : page.getContent()) {
            lines.add(String.join(",",
                escapeCsv(row.id() == null ? "" : row.id().toString()),
                escapeCsv(row.createdAt() == null ? "" : row.createdAt().toString()),
                escapeCsv(row.action()),
                escapeCsv(row.actorUsername()),
                escapeCsv(row.targetUsername()),
                escapeCsv(row.details())
            ));
        }
        return String.join("\n", lines);
    }

    private void validateDateRange(Instant from, Instant to) {
        if (from != null && to != null && to.isBefore(from)) {
            throw new IllegalArgumentException("Audit 'to' time cannot be earlier than 'from' time");
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String escapeCsv(String value) {
        if (value == null) {
            return "\"\"";
        }
        String escaped = value.replace("\"", "\"\"");
        return "\"" + escaped + "\"";
    }

    private AdminAuditLogResponse toResponse(AdminAuditLog log) {
        return new AdminAuditLogResponse(
            log.getId(),
            log.getActorUsername(),
            log.getTargetUsername(),
            log.getAction(),
            log.getDetails(),
            log.getCreatedAt()
        );
    }
}
