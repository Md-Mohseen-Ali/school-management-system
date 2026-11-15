package com.schoolmanagement.modules.identity.controller;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.schoolmanagement.modules.identity.dto.GuardianProfileResponse;
import com.schoolmanagement.modules.identity.service.GuardianDirectoryService;

@RestController
@RequestMapping("/api/v1/identity/guardians")
@PreAuthorize("hasRole('ADMIN')")
public class GuardianDirectoryController {

    private final GuardianDirectoryService guardianDirectoryService;

    public GuardianDirectoryController(
            GuardianDirectoryService guardianDirectoryService
    ) {
        this.guardianDirectoryService = guardianDirectoryService;
    }

    @GetMapping
    public List<GuardianProfileResponse> getAllGuardians() {
        return guardianDirectoryService.getAllGuardians();
    }
}