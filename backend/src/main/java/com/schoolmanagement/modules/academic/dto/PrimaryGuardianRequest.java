package com.schoolmanagement.modules.academic.dto;

import jakarta.validation.constraints.NotNull;

public record PrimaryGuardianRequest(

        @NotNull(message = "Primary status is required")
        Boolean primary

) {
}