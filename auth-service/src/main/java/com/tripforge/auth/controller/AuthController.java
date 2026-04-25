package com.tripforge.auth.controller;

import com.tripforge.auth.dto.ApiResponse;
import com.tripforge.auth.dto.AuthResponse;
import com.tripforge.auth.dto.LoginRequest;
import com.tripforge.auth.dto.RegisterRequest;
import com.tripforge.auth.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Authentication controller.
 * All endpoints here are PUBLIC (no JWT required).
 *
 * POST /api/auth/register  — create a new account
 * POST /api/auth/login     — authenticate and receive JWT
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    /**
     * Register a new user.
     *
     * Request:
     * POST /api/auth/register
     * {
     *   "email": "arjun@example.com",
     *   "password": "SecurePass@123",
     *   "firstName": "Arjun",
     *   "lastName": "Sharma",
     *   "phone": "9876543210"
     * }
     *
     * Response 201:
     * {
     *   "success": true,
     *   "message": "Registration successful",
     *   "data": {
     *     "token": "eyJhbGci...",
     *     "tokenType": "Bearer",
     *     "userId": 1,
     *     "email": "arjun@example.com",
     *     "firstName": "Arjun",
     *     "lastName": "Sharma",
     *     "expiresIn": 86400000
     *   }
     * }
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Registration successful", response));
    }

    /**
     * Login with email and password.
     *
     * Request:
     * POST /api/auth/login
     * {
     *   "email": "arjun@example.com",
     *   "password": "SecurePass@123"
     * }
     *
     * Response 200:
     * {
     *   "success": true,
     *   "message": "Login successful",
     *   "data": {
     *     "token": "eyJhbGci...",
     *     "tokenType": "Bearer",
     *     "userId": 1,
     *     "email": "arjun@example.com",
     *     "firstName": "Arjun",
     *     "lastName": "Sharma",
     *     "expiresIn": 86400000
     *   }
     * }
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("Login successful", response));
    }
}
