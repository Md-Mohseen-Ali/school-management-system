package com.schoolmanagement.modules.identity.dto;

import java.time.LocalDate;

import com.schoolmanagement.modules.identity.entity.UserRole;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateIdentityAccountRequest(
        @NotNull UserRole role,
        @NotBlank @Size(max = 120) String fullName,
        @NotBlank @Email @Size(max = 180) String email,
        @Size(max = 25) String phone,
        @NotBlank @Size(min = 12, max = 72) String initialPassword,
        @Size(max = 30) String employeeCode,
        LocalDate hireDate,
        @Size(max = 25) String alternatePhone
) {
}
