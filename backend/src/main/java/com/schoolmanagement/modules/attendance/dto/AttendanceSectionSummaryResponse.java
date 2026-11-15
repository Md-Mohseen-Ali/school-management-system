package com.schoolmanagement.modules.attendance.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record AttendanceSectionSummaryResponse(

        Long sectionId,
        String sectionName,
        String className,
        String academicYear,

        LocalDate fromDate,
        LocalDate toDate,

        Long totalSessions,
        Long totalStudents,
        Long totalRecords,

        Long presentCount,
        Long absentCount,
        Long lateCount,
        Long excusedCount,

        BigDecimal attendancePercentage,

        List<AttendanceStudentSummaryResponse> students

) {
}