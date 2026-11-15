package com.schoolmanagement.modules.examination.dto;

import jakarta.validation.constraints.NotNull;

import com.schoolmanagement.modules.examination.entity.ExaminationStatus;

public record ExaminationStatusRequest(

        @NotNull(message = "Examination status is required")
        ExaminationStatus status

) {
}