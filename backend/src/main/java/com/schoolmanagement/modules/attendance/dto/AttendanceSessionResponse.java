package com.schoolmanagement.modules.attendance.dto;

import java.time.Instant;
import java.time.LocalDate;

import com.schoolmanagement.modules.attendance.entity.AttendanceSessionStatus;

public record AttendanceSessionResponse(

        Long id,

        Long sectionId,
        String sectionName,
        String className,
        String academicYear,

        Long teacherId,
        String employeeCode,

        LocalDate attendanceDate,
        String sessionCode,

        AttendanceSessionStatus status,
        String notes,

        Instant createdAt,
        Instant submittedAt

) {
}