package com.schoolmanagement.modules.fees.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.schoolmanagement.modules.fees.entity.PaymentMethod;

public record FeePaymentRequest(

        @NotNull(message = "Payment amount is required")
        @DecimalMin(value = "0.01", message = "Payment must be positive")
        @Digits(integer = 10, fraction = 2)
        BigDecimal amount,

        @NotNull(message = "Payment method is required")
        PaymentMethod paymentMethod,

        @NotBlank(message = "Idempotency key is required")
        @Size(max = 100)
        String idempotencyKey

) {
}