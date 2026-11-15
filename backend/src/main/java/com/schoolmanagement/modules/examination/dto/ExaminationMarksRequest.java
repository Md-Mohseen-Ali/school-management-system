package com.schoolmanagement.modules.examination.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

public record ExaminationMarksRequest(

        @NotEmpty(message = "At least one student's marks are required")
        List<@Valid ExaminationMarkEntryRequest> records

) {
}