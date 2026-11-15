package com.schoolmanagement.modules.fees.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;

import org.springframework.web.bind.annotation.*;

import com.schoolmanagement.modules.fees.dto.FeeInvoiceItemRequest;
import com.schoolmanagement.modules.fees.dto.FeeInvoiceItemResponse;
import com.schoolmanagement.modules.fees.dto.FeeInvoiceRequest;
import com.schoolmanagement.modules.fees.dto.FeeInvoiceResponse;

import com.schoolmanagement.modules.fees.entity.FeeInvoiceStatus;
import com.schoolmanagement.modules.fees.service.FeeInvoiceService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/fees/invoices")
@PreAuthorize("hasAnyRole('ADMIN', 'GUARDIAN')")
public class FeeInvoiceController {

    private final FeeInvoiceService invoiceService;

    public FeeInvoiceController(FeeInvoiceService invoiceService) {
        this.invoiceService = invoiceService;
    }

    private boolean isAdmin(Authentication authentication) {

        return authentication.getAuthorities().stream()
                .anyMatch(authority ->
                        authority.getAuthority().equals("ROLE_ADMIN"));
    }

    // CREATE DRAFT INVOICE
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public FeeInvoiceResponse createInvoice(
            @Valid @RequestBody FeeInvoiceRequest request,
            Authentication authentication
    ) {
        return invoiceService.createInvoice(
                request,
                authentication.getName()
        );
    }

    // ADD INVOICE ITEM
    @PostMapping("/{invoiceId}/items")
    @PreAuthorize("hasRole('ADMIN')")
    public FeeInvoiceItemResponse addInvoiceItem(
            @PathVariable Long invoiceId,
            @Valid @RequestBody FeeInvoiceItemRequest request
    ) {
        return invoiceService.addInvoiceItem(invoiceId, request);
    }

    // ISSUE INVOICE
    @PostMapping("/{invoiceId}/issue")
    @PreAuthorize("hasRole('ADMIN')")
    public FeeInvoiceResponse issueInvoice(
            @PathVariable Long invoiceId
    ) {
        return invoiceService.issueInvoice(invoiceId);
    }

    // CANCEL INVOICE
    @PostMapping("/{invoiceId}/cancel")
    @PreAuthorize("hasRole('ADMIN')")
    public FeeInvoiceResponse cancelInvoice(
            @PathVariable Long invoiceId
    ) {
        return invoiceService.cancelInvoice(invoiceId);
    }

    // LIST INVOICES
    @GetMapping
    public Page<FeeInvoiceResponse> getAllInvoices(
            @RequestParam(required = false) Long studentId,
            @RequestParam(required = false) String academicYear,
            @RequestParam(required = false) FeeInvoiceStatus status,

            @PageableDefault(
                size = 10,
                sort = "createdAt",
                direction = Sort.Direction.DESC
            )
            Pageable pageable,

            Authentication authentication
    ) {
        return invoiceService.getAllInvoices(
                studentId,
                academicYear,
                status,
                authentication.getName(),
                isAdmin(authentication),
                pageable
        );
    }

    // GET INVOICE BY ID
    @GetMapping("/{invoiceId}")
    public FeeInvoiceResponse getInvoice(
            @PathVariable Long invoiceId,
            Authentication authentication
    ) {
        return invoiceService.getInvoice(
                invoiceId,
                authentication.getName(),
                isAdmin(authentication)
        );
    }
}