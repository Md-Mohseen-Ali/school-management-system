package com.schoolmanagement.modules.identity.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ApproveAccountAccessRequest(
        @NotBlank @Size(min = 12, max = 72) String initialPassword,
        @AssertTrue(message = "Confirm that the applicant's identity was verified outside the application")
        boolean identityVerified,
        @NotBlank @Size(max = 500) String reviewNote
) {
}
