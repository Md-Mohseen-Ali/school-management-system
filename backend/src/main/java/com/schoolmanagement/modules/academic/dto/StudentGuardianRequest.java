package com.schoolmanagement.modules.academic.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record StudentGuardianRequest(

        @NotNull(message = "Student ID is required")
        Long studentId,

        @NotNull(message = "Guardian ID is required")
        Long guardianId,

        @NotBlank(message = "Relationship is required")
        @Size(max = 30)
        String relationship,

        boolean primary

) {
}