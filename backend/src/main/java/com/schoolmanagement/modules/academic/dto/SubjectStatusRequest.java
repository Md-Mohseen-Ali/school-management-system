package com.schoolmanagement.modules.academic.dto;

import jakarta.validation.constraints.NotNull;

public record SubjectStatusRequest(

        @NotNull(message = "Active status is required")
        Boolean active

) {
}