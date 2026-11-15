package com.schoolmanagement.modules.academic.dto;

import jakarta.validation.constraints.NotNull;

public record AssignmentStatusRequest(

        @NotNull(message = "Active status is required")
        Boolean active

) {
}