package com.schoolmanagement.modules.identity.dto;

public record AuthResponse(
        String accessToken,
        String tokenType,
        long expiresIn
) {
}