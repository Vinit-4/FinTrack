package com.fintrack.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/** What the frontend receives after a successful register or login. */
@Data
@AllArgsConstructor
public class AuthResponse {
    private String token;
    private String tokenType;
    private Long userId;
    private String name;
    private String email;
}
