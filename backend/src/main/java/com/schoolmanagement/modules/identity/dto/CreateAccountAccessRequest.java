package com.schoolmanagement.modules.identity.dto;

import java.time.LocalDate;

import com.schoolmanagement.modules.identity.entity.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

public record CreateAccountAccessRequest(
        @NotNull UserRole requestedRole,
        @NotBlank @Size(max = 120) String fullName,
        @NotBlank @Email @Size(max = 180) String email,
        @Size(max = 25) String phone,
        @Size(max = 30) String employeeCode,
        LocalDate hireDate,
        @Size(max = 25) String alternatePhone,
        @Size(max = 40) String admissionNumber,
        @Size(max = 80) String firstName,
        @Size(max = 80) String lastName,
        @Past LocalDate dateOfBirth,
        @Size(max = 40) String studentAdmissionNumber,
        @Size(max = 30) String relationship
) {
}
