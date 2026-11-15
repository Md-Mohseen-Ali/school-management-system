package com.schoolmanagement.modules.identity.dto;

public record GuardianProfileResponse(
        Long id,
        Long userId,
        String alternatePhone
) {
}