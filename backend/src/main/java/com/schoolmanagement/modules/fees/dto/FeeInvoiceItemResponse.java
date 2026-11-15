package com.schoolmanagement.modules.fees.dto;

import java.math.BigDecimal;

public record FeeInvoiceItemResponse(
        Long id,
        String description,
        BigDecimal amount
) {
}