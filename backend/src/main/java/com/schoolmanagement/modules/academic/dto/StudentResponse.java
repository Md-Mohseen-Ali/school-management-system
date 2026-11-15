package com.schoolmanagement.modules.academic.dto;

import java.time.LocalDate;

import com.schoolmanagement.modules.academic.entity.StudentStatus;

public record StudentResponse(
        Long id,
        String admissionNumber,
        String firstName,
        String lastName,
        LocalDate dateOfBirth,
        LocalDate admissionDate,
        StudentStatus status,
        String email,
        boolean portalAccessEnabled
) {
    // Compatibility constructor for linked-student views that should not
    // disclose the student's login email to another account.
    public StudentResponse(
            Long id,
            String admissionNumber,
            String firstName,
            String lastName,
            LocalDate dateOfBirth,
            LocalDate admissionDate,
            StudentStatus status) {
        this(id, admissionNumber, firstName, lastName, dateOfBirth,
                admissionDate, status, null, false);
    }
}
