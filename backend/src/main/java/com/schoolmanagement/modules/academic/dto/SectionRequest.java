package com.schoolmanagement.modules.academic.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SectionRequest(

        @NotNull(message = "Class ID is required")
        Long schoolClassId,

        @NotBlank(message = "Section name is required")
        @Size(max = 20)
        String sectionName,

        @NotBlank(message = "Academic year is required")
        @Pattern(
            regexp = "\\d{4}-\\d{4}",
            message = "Academic year must follow YYYY-YYYY format"
        )
        String academicYear

) {
}