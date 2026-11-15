package com.schoolmanagement.modules.examination.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Digits;

public record ExaminationSubjectRequest(

        @NotNull(message = "Subject ID is required")
        Long subjectId,

        @NotNull(message = "Examination date is required")
        LocalDate examinationDate,

        @NotNull
        @DecimalMin(value = "0.01")
        @Digits(integer = 5, fraction = 2)
        BigDecimal maxMarks,

        @NotNull
        @DecimalMin(value = "0.00")
        @Digits(integer = 5, fraction = 2)
        BigDecimal passMarks

) {
}