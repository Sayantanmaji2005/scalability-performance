package com.scalemart.api.controller;

import com.scalemart.api.dto.AdminUserResponse;
import com.scalemart.api.dto.UpdateUserEnabledRequest;
import com.scalemart.api.dto.UpdateUserRoleRequest;
import com.scalemart.api.service.AdminUserService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/users")
public class AdminUserController {

    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    @GetMapping
    public List<AdminUserResponse> listUsers(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) Boolean enabled) {
        return adminUserService.listUsers(query, role, enabled);
    }

    @PatchMapping("/{userId}/enabled")
    public AdminUserResponse updateEnabled(
            @PathVariable Long userId,
            @Valid @RequestBody UpdateUserEnabledRequest request,
            Principal principal) {
        return adminUserService.updateEnabled(userId, request.enabled(), principal.getName());
    }

    @PatchMapping("/{userId}/role")
    public AdminUserResponse updateRole(
            @PathVariable Long userId,
            @Valid @RequestBody UpdateUserRoleRequest request,
            Principal principal) {
        return adminUserService.updateRole(userId, request.role(), principal.getName());
    }
}
