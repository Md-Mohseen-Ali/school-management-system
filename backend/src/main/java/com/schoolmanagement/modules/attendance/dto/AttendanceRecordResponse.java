package com.schoolmanagement.modules.attendance.dto;

import java.time.Instant;

import com.schoolmanagement.modules.attendance.entity.AttendanceRecordStatus;

public record AttendanceRecordResponse(

        Long id,

        Long studentId,
        String admissionNumber,
        String studentName,

        AttendanceRecordStatus attendanceStatus,
        String remarks,

        Instant markedAt,
        Instant updatedAt

) {
}