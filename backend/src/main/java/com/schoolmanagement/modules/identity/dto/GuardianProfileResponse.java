package com.schoolmanagement.modules.identity.dto;

public record GuardianProfileResponse(
        Long id,
        Long userId,
        String fullName,
        String email,
        String phone,
        String alternatePhone
) {
}
