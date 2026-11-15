package com.schoolmanagement.modules.academic.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.schoolmanagement.modules.academic.dto.PrimaryGuardianRequest;
import com.schoolmanagement.modules.academic.dto.StudentGuardianRequest;
import com.schoolmanagement.modules.academic.dto.StudentGuardianResponse;
import com.schoolmanagement.modules.academic.service.StudentGuardianService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/academic/student-guardians")
@PreAuthorize("hasRole('ADMIN')")
public class StudentGuardianController {

    private final StudentGuardianService guardianService;

    public StudentGuardianController(
            StudentGuardianService guardianService
    ) {
        this.guardianService = guardianService;
    }

    @PostMapping
    public StudentGuardianResponse linkGuardian(
            @Valid @RequestBody StudentGuardianRequest request
    ) {
        return guardianService.linkGuardian(request);
    }

    @GetMapping
    public Page<StudentGuardianResponse> getAllLinks(
            @RequestParam(required = false) Long studentId,
            @RequestParam(required = false) Long guardianId,
            @PageableDefault(size = 10) Pageable pageable
    ) {
        return guardianService.getAllLinks(
                studentId,
                guardianId,
                pageable
        );
    }

    @PatchMapping("/{studentId}/{guardianId}/primary")
    public StudentGuardianResponse updatePrimaryStatus(
            @PathVariable Long studentId,
            @PathVariable Long guardianId,
            @Valid @RequestBody PrimaryGuardianRequest request
    ) {
        return guardianService.updatePrimaryStatus(
                studentId,
                guardianId,
                request.primary()
        );
    }

    @DeleteMapping("/{studentId}/{guardianId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unlinkGuardian(
            @PathVariable Long studentId,
            @PathVariable Long guardianId
    ) {
        guardianService.unlinkGuardian(studentId, guardianId);
    }
}