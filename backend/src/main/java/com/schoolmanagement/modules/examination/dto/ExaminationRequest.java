package com.schoolmanagement.modules.examination.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ExaminationRequest(

        @NotNull(message = "Section ID is required")
        Long sectionId,

        @NotBlank(message = "Examination name is required")
        @Size(max = 100)
        String examName,

        @NotBlank(message = "Academic year is required")
        @Pattern(regexp = "\\d{4}-\\d{4}")
        String academicYear,

        @NotNull(message = "Start date is required")
        LocalDate startDate,

        @NotNull(message = "End date is required")
        LocalDate endDate

) {
}