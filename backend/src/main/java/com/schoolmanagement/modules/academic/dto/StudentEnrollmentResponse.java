package com.schoolmanagement.modules.academic.dto;

import java.time.LocalDate;

public record StudentEnrollmentResponse(

        Long id,

        Long studentId,
        String admissionNumber,
        String studentName,

        Long sectionId,
        String sectionName,
        String className,
        String academicYear,

        LocalDate startDate,
        LocalDate endDate,
        boolean active

) {
}