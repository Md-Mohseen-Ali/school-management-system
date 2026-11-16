package com.schoolmanagement.modules.identity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ApproveAccountAccessRequest(
        @NotBlank @Size(min = 12, max = 72) String initialPassword,
        @Size(max = 500) String reviewNote
) {
}
