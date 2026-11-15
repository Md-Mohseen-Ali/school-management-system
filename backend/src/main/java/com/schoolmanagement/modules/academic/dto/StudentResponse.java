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
        StudentStatus status

) {
}