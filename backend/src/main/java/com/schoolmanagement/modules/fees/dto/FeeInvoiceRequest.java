package com.schoolmanagement.modules.fees.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record FeeInvoiceRequest(

        @NotBlank(message = "Invoice number is required")
        @Size(max = 40)
        String invoiceNumber,

        @NotNull(message = "Student ID is required")
        Long studentId,

        @NotBlank(message = "Academic year is required")
        @Pattern(regexp = "\\d{4}-\\d{4}")
        String academicYear,

        @NotNull(message = "Issue date is required")
        LocalDate issueDate,

        @NotNull(message = "Due date is required")
        LocalDate dueDate

) {
}