package com.schoolmanagement.modules.academic.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;

public record StudentEnrollmentRequest(

        @NotNull(message = "Student ID is required")
        Long studentId,

        @NotNull(message = "Section ID is required")
        Long sectionId,

        LocalDate startDate

) {
}