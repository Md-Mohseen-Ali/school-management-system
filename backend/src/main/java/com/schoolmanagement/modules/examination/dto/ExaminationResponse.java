package com.schoolmanagement.modules.examination.dto;

import java.time.Instant;
import java.time.LocalDate;

import com.schoolmanagement.modules.examination.entity.ExaminationStatus;

public record ExaminationResponse(

        Long id,

        Long sectionId,
        String sectionName,
        String className,

        String examName,
        String academicYear,

        LocalDate startDate,
        LocalDate endDate,

        ExaminationStatus status,
        Instant createdAt

) {
}