package com.schoolmanagement.modules.identity.controller;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.schoolmanagement.modules.identity.dto.CreateIdentityAccountRequest;
import com.schoolmanagement.modules.identity.dto.IdentityAccountResponse;
import com.schoolmanagement.modules.identity.service.IdentityAccountService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/identity/accounts")
@PreAuthorize("hasRole('ADMIN')")
public class IdentityAccountController {

    private final IdentityAccountService accountService;

    public IdentityAccountController(IdentityAccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public IdentityAccountResponse createAccount(
            @Valid @RequestBody CreateIdentityAccountRequest request) {
        return accountService.createAccount(request);
    }
}
