package com.schoolmanagement.modules.attendance.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

public record AttendanceRecordsRequest(

        @NotEmpty(message = "At least one attendance entry is required")
        List<@Valid AttendanceEntryRequest> records

) {
}