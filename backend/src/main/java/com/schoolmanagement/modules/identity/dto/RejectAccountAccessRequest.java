package com.schoolmanagement.modules.identity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RejectAccountAccessRequest(
        @NotBlank @Size(max = 500) String reviewNote
) {
}
