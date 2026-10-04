package com.marketflow.controller;

import com.marketflow.dto.ApiResponse;
import com.marketflow.dto.auth.*;
import com.marketflow.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/auth")
@Tag(name = "Authentication", description = "User registration, login, and JWT token rotation endpoints")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @Operation(summary = "Register user", description = "Creates a new user account and returns JWT token pair")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("User registered successfully", response));
    }

    @PostMapping("/login")
    @Operation(summary = "Login user", description = "Authenticates credentials and returns JWT token pair")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("Login successful", response));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh token", description = "Rotates refresh token and issues fresh access and refresh tokens")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        AuthResponse response = authService.refreshToken(request);
        return ResponseEntity.ok(ApiResponse.success("Token refreshed successfully", response));
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout user", description = "Revokes the active refresh token for the current device")
    public ResponseEntity<ApiResponse<Void>> logout(@RequestBody(required = false) RefreshTokenRequest request) {
        if (request != null) {
            authService.logout(request);
        }
        return ResponseEntity.ok(ApiResponse.success("Logged out successfully from this device", null));
    }

    @PostMapping("/logout-all")
    @Operation(summary = "Logout all devices", description = "Revokes all refresh tokens and sessions for the user")
    public ResponseEntity<ApiResponse<Void>> logoutAll(Principal principal) {
        if (principal != null) {
            authService.logoutAll(principal.getName());
        }
        return ResponseEntity.ok(ApiResponse.success("Logged out successfully from all devices", null));
    }

    @GetMapping("/me")
    @Operation(summary = "Current user profile", description = "Returns the profile of the currently authenticated user")
    public ResponseEntity<ApiResponse<UserSummaryDto>> getCurrentUser(Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        UserSummaryDto user = authService.getCurrentUser(principal.getName());
        return ResponseEntity.ok(ApiResponse.success("User profile retrieved successfully", user));
    }
}
