package com.schoolmanagement.modules.identity.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;

import com.schoolmanagement.modules.identity.dto.*;
import com.schoolmanagement.modules.identity.entity.AccountRequestStatus;
import com.schoolmanagement.modules.identity.service.AccountAccessRequestService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/account-requests")
public class AccountAccessRequestController {

    private final AccountAccessRequestService requestService;

    public AccountAccessRequestController(AccountAccessRequestService requestService) {
        this.requestService = requestService;
    }

    @PostMapping
    @PreAuthorize("permitAll()")
    @ResponseStatus(HttpStatus.CREATED)
    public AccountAccessRequestSubmissionResponse submit(
            @Valid @RequestBody CreateAccountAccessRequest request,
            HttpServletRequest servletRequest) {
        return requestService.submit(request, servletRequest.getRemoteAddr());
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<AccountAccessRequestResponse> getRequests(
            @RequestParam(required = false) AccountRequestStatus status) {
        return requestService.getRequests(status);
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public AccountAccessRequestResponse approve(
            @PathVariable Long id,
            @Valid @RequestBody ApproveAccountAccessRequest review,
            Authentication authentication) {
        return requestService.approve(id, review, authentication.getName());
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public AccountAccessRequestResponse reject(
            @PathVariable Long id,
            @Valid @RequestBody RejectAccountAccessRequest review,
            Authentication authentication) {
        return requestService.reject(id, review, authentication.getName());
    }
}
