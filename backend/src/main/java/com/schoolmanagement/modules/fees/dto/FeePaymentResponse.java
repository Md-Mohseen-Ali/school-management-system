package com.schoolmanagement.modules.fees.dto;

import java.math.BigDecimal;
import java.time.Instant;

import com.schoolmanagement.modules.fees.entity.PaymentMethod;
import com.schoolmanagement.modules.fees.entity.PaymentStatus;

public record FeePaymentResponse(

        Long id,
        Long invoiceId,

        String invoiceNumber,
        String paymentReference,

        BigDecimal amount,

        PaymentMethod paymentMethod,
        PaymentStatus paymentStatus,

        Instant paidAt,
        Instant createdAt

) {
}