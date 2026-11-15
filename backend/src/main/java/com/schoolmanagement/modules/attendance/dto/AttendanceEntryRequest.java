package com.schoolmanagement.modules.attendance.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.schoolmanagement.modules.attendance.entity.AttendanceRecordStatus;

public record AttendanceEntryRequest(

        @NotNull(message = "Student ID is required")
        Long studentId,

        @NotNull(message = "Attendance status is required")
        AttendanceRecordStatus attendanceStatus,

        @Size(max = 255, message = "Remarks cannot exceed 255 characters")
        String remarks

) {
}