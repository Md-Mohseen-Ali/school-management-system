package com.schoolmanagement.modules.identity.dto;

import com.schoolmanagement.modules.identity.entity.UserRole;

public record IdentityAccountResponse(
        Long userId,
        Long profileId,
        String fullName,
        String email,
        UserRole role,
        String employeeCode
) {
}
