package com.schoolmanagement.modules.examination.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record GradingScaleRequest(

        @NotBlank
        @Pattern(regexp = "\\d{4}-\\d{4}")
        String academicYear,

        @NotBlank
        @Size(max = 5)
        String grade,

        @NotNull
        @DecimalMin("0.00")
        @DecimalMax("100.00")
        @Digits(integer = 3, fraction = 2)
        BigDecimal minPercentage,

        @NotNull
        @DecimalMin("0.00")
        @DecimalMax("100.00")
        @Digits(integer = 3, fraction = 2)
        BigDecimal maxPercentage,

        @Size(max = 100)
        String remarks

) {
}