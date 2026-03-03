package com.scalemart.api.service;

import com.scalemart.api.domain.User;
import com.scalemart.api.dto.AdminUserResponse;
import com.scalemart.api.repository.UserRepository;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminUserService {

    private static final String ROLE_ADMIN = "ADMIN";
    private static final String ROLE_USER = "USER";

    private final UserRepository userRepository;
    private final AdminAuditService adminAuditService;

    public AdminUserService(UserRepository userRepository, AdminAuditService adminAuditService) {
        this.userRepository = userRepository;
        this.adminAuditService = adminAuditService;
    }

    public List<AdminUserResponse> listUsers(String query, String role, Boolean enabled) {
        String normalizedQuery = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        String normalizedRole = normalizeRole(role, false);

        Stream<User> stream = userRepository.findAll().stream();

        if (!normalizedQuery.isEmpty()) {
            stream = stream.filter(user ->
                containsIgnoreCase(user.getUsername(), normalizedQuery)
                    || containsIgnoreCase(user.getEmail(), normalizedQuery));
        }

        if (normalizedRole != null) {
            stream = stream.filter(user -> normalizedRole.equals(user.getRole()));
        }

        if (enabled != null) {
            stream = stream.filter(user -> enabled.equals(user.isEnabled()));
        }

        return stream
            .sorted(Comparator.comparing(User::getCreatedAt).reversed())
            .map(this::toResponse)
            .toList();
    }

    @Transactional
    public AdminUserResponse updateEnabled(Long userId, Boolean enabled, String actorUsername) {
        if (enabled == null) {
            throw new IllegalArgumentException("Enabled is required");
        }

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        boolean previousEnabled = user.isEnabled();

        if (user.getUsername().equals(actorUsername) && !enabled) {
            throw new IllegalArgumentException("You cannot disable your own account");
        }

        validateLastAdminProtection(user, enabled, user.getRole());
        user.setEnabled(enabled);
        User saved = userRepository.save(user);
        adminAuditService.record(
            actorUsername,
            "USER_ENABLED_UPDATED",
            saved.getUsername(),
            "enabled:" + previousEnabled + "->" + saved.isEnabled()
        );
        return toResponse(saved);
    }

    @Transactional
    public AdminUserResponse updateRole(Long userId, String role, String actorUsername) {
        String normalizedRole = normalizeRole(role, true);
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        String previousRole = user.getRole();

        if (user.getUsername().equals(actorUsername) && !ROLE_ADMIN.equals(normalizedRole)) {
            throw new IllegalArgumentException("You cannot remove your own ADMIN role");
        }

        validateLastAdminProtection(user, user.isEnabled(), normalizedRole);
        user.setRole(normalizedRole);
        User saved = userRepository.save(user);
        adminAuditService.record(
            actorUsername,
            "USER_ROLE_UPDATED",
            saved.getUsername(),
            "role:" + previousRole + "->" + saved.getRole()
        );
        return toResponse(saved);
    }

    private void validateLastAdminProtection(User user, boolean newEnabled, String newRole) {
        if (!ROLE_ADMIN.equals(user.getRole()) || !user.isEnabled()) {
            return;
        }

        boolean losingAdmin = !ROLE_ADMIN.equals(newRole) || !newEnabled;
        if (!losingAdmin) {
            return;
        }

        long enabledAdminCount = userRepository.countByRoleAndEnabledTrue(ROLE_ADMIN);
        if (enabledAdminCount <= 1) {
            throw new IllegalArgumentException("Cannot disable or demote the last enabled admin account");
        }
    }

    private String normalizeRole(String role, boolean required) {
        if (role == null || role.isBlank()) {
            if (required) {
                throw new IllegalArgumentException("Role is required");
            }
            return null;
        }

        String normalized = role.trim().toUpperCase(Locale.ROOT);
        if (!ROLE_USER.equals(normalized) && !ROLE_ADMIN.equals(normalized)) {
            throw new IllegalArgumentException("Role must be USER or ADMIN");
        }
        return normalized;
    }

    private boolean containsIgnoreCase(String value, String query) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(query);
    }

    private AdminUserResponse toResponse(User user) {
        return new AdminUserResponse(
            user.getId(),
            user.getUsername(),
            user.getEmail(),
            user.getRole(),
            user.isEnabled(),
            user.getCreatedAt(),
            user.getUpdatedAt()
        );
    }
}
