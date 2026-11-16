package com.schoolmanagement.modules.identity.dto;

import java.time.Instant;
import java.time.LocalDate;

import com.schoolmanagement.modules.identity.entity.AccountRequestStatus;
import com.schoolmanagement.modules.identity.entity.UserRole;

public record AccountAccessRequestResponse(
        Long id,
        UserRole requestedRole,
        String fullName,
        String email,
        String phone,
        String employeeCode,
        LocalDate hireDate,
        String alternatePhone,
        String admissionNumber,
        String firstName,
        String lastName,
        LocalDate dateOfBirth,
        String studentAdmissionNumber,
        String relationship,
        AccountRequestStatus status,
        String reviewNote,
        Long reviewedByUserId,
        Instant reviewedAt,
        Long provisionedEntityId,
        Instant createdAt
) {
}
