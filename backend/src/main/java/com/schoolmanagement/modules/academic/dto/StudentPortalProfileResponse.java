package com.schoolmanagement.modules.academic.dto;

import java.time.LocalDate;

import com.schoolmanagement.modules.academic.entity.StudentStatus;

public record StudentPortalProfileResponse(
        Long studentId,
        String admissionNumber,
        String firstName,
        String lastName,
        LocalDate dateOfBirth,
        LocalDate admissionDate,
        StudentStatus status,
        String email,
        String phone,
        Long sectionId,
        String sectionName,
        String className,
        String academicYear,
        LocalDate enrollmentStartDate
) {
}
