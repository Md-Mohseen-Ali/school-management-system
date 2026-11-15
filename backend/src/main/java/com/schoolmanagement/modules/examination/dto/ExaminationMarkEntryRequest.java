package com.schoolmanagement.modules.examination.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.schoolmanagement.modules.examination.entity.ExamAttendanceStatus;

public record ExaminationMarkEntryRequest(

        @NotNull(message = "Student ID is required")
        Long studentId,

        @NotNull(message = "Marks obtained are required")
        @DecimalMin(value = "0.00", message = "Marks cannot be negative")
        @Digits(integer = 5, fraction = 2)
        BigDecimal marksObtained,

        @NotNull(message = "Attendance status is required")
        ExamAttendanceStatus attendanceStatus,

        @Size(max = 255, message = "Remarks cannot exceed 255 characters")
        String remarks

) {
}