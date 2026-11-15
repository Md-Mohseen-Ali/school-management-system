package com.schoolmanagement.modules.fees.controller;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.schoolmanagement.modules.fees.dto.FeePaymentRequest;
import com.schoolmanagement.modules.fees.dto.FeePaymentResponse;
import com.schoolmanagement.modules.fees.service.FeePaymentService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/fees/invoices/{invoiceId}/payments")
@PreAuthorize("hasAnyRole('ADMIN', 'GUARDIAN')")
public class FeePaymentController {

    private final FeePaymentService paymentService;

    public FeePaymentController(FeePaymentService paymentService) {
        this.paymentService = paymentService;
    }

    private boolean isAdmin(Authentication authentication) {

        return authentication.getAuthorities().stream()
                .anyMatch(authority ->
                        authority.getAuthority().equals("ROLE_ADMIN"));
    }

    @PostMapping
    public FeePaymentResponse processPayment(
            @PathVariable Long invoiceId,
            @Valid @RequestBody FeePaymentRequest request,
            Authentication authentication
    ) {
        return paymentService.processPayment(
                invoiceId,
                request,
                authentication.getName(),
                isAdmin(authentication)
        );
    }

    @GetMapping
    public List<FeePaymentResponse> getPaymentHistory(
            @PathVariable Long invoiceId,
            Authentication authentication
    ) {
        return paymentService.getPaymentHistory(
                invoiceId,
                authentication.getName(),
                isAdmin(authentication)
        );
    }
}