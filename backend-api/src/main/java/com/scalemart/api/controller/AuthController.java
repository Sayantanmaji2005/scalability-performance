package com.scalemart.api.controller;

import com.scalemart.api.dto.AuthActionResponse;
import com.scalemart.api.dto.ChangePasswordRequest;
import com.scalemart.api.dto.ForgotPasswordRequest;
import com.scalemart.api.dto.LoginRequest;
import com.scalemart.api.dto.LoginResponse;
import com.scalemart.api.dto.RefreshTokenRequest;
import com.scalemart.api.dto.RegisterRequest;
import com.scalemart.api.dto.ResendVerificationRequest;
import com.scalemart.api.dto.ResetPasswordRequest;
import com.scalemart.api.dto.VerifyEmailRequest;
import com.scalemart.api.service.AuthService;
import jakarta.validation.Valid;
import java.security.Principal;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request.username(), request.password());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/register")
    public ResponseEntity<AuthActionResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthActionResponse response = authService.register(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/verify-email")
    public ResponseEntity<AuthActionResponse> verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        AuthActionResponse response = authService.verifyEmail(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<AuthActionResponse> resendVerification(
            @Valid @RequestBody ResendVerificationRequest request) {
        AuthActionResponse response = authService.resendVerification(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<AuthActionResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        AuthActionResponse response = authService.forgotPassword(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/reset-password")
    public ResponseEntity<AuthActionResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        AuthActionResponse response = authService.resetPassword(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/change-password")
    public ResponseEntity<AuthActionResponse> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            Principal principal) {
        AuthActionResponse response = authService.changePassword(principal.getName(), request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        LoginResponse response = authService.refreshToken(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @RequestHeader("Authorization") String authorizationHeader,
            @AuthenticationPrincipal String username) {
        String token = authorizationHeader.substring(7);
        authService.logout(token, username);
        return ResponseEntity.ok().build();
    }
}
