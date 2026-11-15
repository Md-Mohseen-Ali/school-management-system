package com.schoolmanagement.modules.academic.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SubjectRequest(

        @NotBlank(message = "Subject code is required")
        @Size(max = 30, message = "Subject code cannot exceed 30 characters")
        @Pattern(
            regexp = "[A-Za-z0-9][A-Za-z0-9_-]*",
            message = "Subject code can contain letters, numbers, hyphens and underscores"
        )
        String subjectCode,

        @NotBlank(message = "Subject name is required")
        @Size(max = 100, message = "Subject name cannot exceed 100 characters")
        String name

) {
}