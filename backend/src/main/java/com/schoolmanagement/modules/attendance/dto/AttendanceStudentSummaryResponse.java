package com.schoolmanagement.modules.attendance.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AttendanceStudentSummaryResponse(

        Long studentId,
        String admissionNumber,
        String studentName,

        LocalDate fromDate,
        LocalDate toDate,

        Long totalRecords,
        Long presentCount,
        Long absentCount,
        Long lateCount,
        Long excusedCount,

        BigDecimal attendancePercentage

) {
}