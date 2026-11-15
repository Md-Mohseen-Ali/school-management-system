package com.schoolmanagement.modules.academic.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SchoolClassRequest(

        @NotBlank(message = "Class name is required")
        @Size(max = 50, message = "Class name cannot exceed 50 characters")
        String name,

        @Size(max = 255, message = "Description cannot exceed 255 characters")
        String description

) {
}