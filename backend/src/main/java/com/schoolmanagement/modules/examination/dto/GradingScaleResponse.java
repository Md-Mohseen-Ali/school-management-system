package com.schoolmanagement.modules.examination.dto;

import java.math.BigDecimal;

public record GradingScaleResponse(

        Long id,
        String academicYear,
        String grade,
        BigDecimal minPercentage,
        BigDecimal maxPercentage,
        String remarks

) {
}