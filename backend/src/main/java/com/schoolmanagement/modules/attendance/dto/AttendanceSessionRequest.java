package com.schoolmanagement.modules.attendance.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AttendanceSessionRequest(

        @NotNull(message = "Section ID is required")
        Long sectionId,

        @NotNull(message = "Attendance date is required")
        LocalDate attendanceDate,

        @Size(max = 20, message = "Session code cannot exceed 20 characters")
        String sessionCode,

        @Size(max = 255, message = "Notes cannot exceed 255 characters")
        String notes

) {
}