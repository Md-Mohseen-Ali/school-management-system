package com.schoolmanagement.modules.attendance.dto;

import java.time.Instant;
import java.time.LocalDate;

import com.schoolmanagement.modules.attendance.entity.AttendanceRecordStatus;
import com.schoolmanagement.modules.attendance.entity.AttendanceSessionStatus;

public record StudentAttendanceHistoryResponse(
        Long attendanceRecordId,
        Long sessionId,
        LocalDate attendanceDate,
        String sessionCode,
        String className,
        String sectionName,
        AttendanceSessionStatus sessionStatus,
        AttendanceRecordStatus attendanceStatus,
        String remarks,
        Instant markedAt
) {
}
