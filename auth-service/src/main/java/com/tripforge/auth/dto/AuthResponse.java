package com.tripforge.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO returned after successful register or login.
 *
 * Sample response:
 * {
 *   "token": "eyJhbGciOiJIUzI1NiJ9...",
 *   "tokenType": "Bearer",
 *   "userId": 1,
 *   "email": "arjun@example.com",
 *   "firstName": "Arjun",
 *   "lastName": "Sharma",
 *   "expiresIn": 86400000
 * }
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    private String token;

    @Builder.Default
    private String tokenType = "Bearer";

    private Long userId;
    private String email;
    private String firstName;
    private String lastName;

    /** Token validity in milliseconds */
    private long expiresIn;
}
