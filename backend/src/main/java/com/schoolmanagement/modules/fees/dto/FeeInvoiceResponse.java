package com.schoolmanagement.modules.fees.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import com.schoolmanagement.modules.fees.entity.FeeInvoiceStatus;

public record FeeInvoiceResponse(

        Long id,
        String invoiceNumber,

        Long studentId,
        String admissionNumber,
        String studentName,

        String academicYear,
        LocalDate issueDate,
        LocalDate dueDate,

        FeeInvoiceStatus status,

        List<FeeInvoiceItemResponse> items,

        BigDecimal totalAmount,
        BigDecimal amountPaid,
        BigDecimal balance,

        Instant createdAt

) {
}