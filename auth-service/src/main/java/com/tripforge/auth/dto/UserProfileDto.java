package com.tripforge.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for returning user profile information.
 *
 * Sample response:
 * {
 *   "id": 1,
 *   "email": "arjun@example.com",
 *   "firstName": "Arjun",
 *   "lastName": "Sharma",
 *   "phone": "9876543210",
 *   "role": "USER",
 *   "createdAt": "2024-01-15T10:30:00"
 * }
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileDto {

    private Long id;
    private String email;
    private String firstName;
    private String lastName;
    private String phone;
    private String role;
    private LocalDateTime createdAt;
}
