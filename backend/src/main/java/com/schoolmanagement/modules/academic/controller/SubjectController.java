package com.schoolmanagement.modules.academic.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.schoolmanagement.modules.academic.dto.SubjectRequest;
import com.schoolmanagement.modules.academic.dto.SubjectResponse;
import com.schoolmanagement.modules.academic.dto.SubjectStatusRequest;
import com.schoolmanagement.modules.academic.service.SubjectService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/academic/subjects")
@PreAuthorize("hasRole('ADMIN')")
public class SubjectController {

    private final SubjectService subjectService;

    public SubjectController(SubjectService subjectService) {
        this.subjectService = subjectService;
    }

    @PostMapping
    public SubjectResponse createSubject(
            @Valid @RequestBody SubjectRequest request
    ) {
        return subjectService.createSubject(request);
    }

    @GetMapping
    public Page<SubjectResponse> getAllSubjects(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean active,
            @PageableDefault(size = 10, sort = "name")
            Pageable pageable
    ) {
        return subjectService.getAllSubjects(search, active, pageable);
    }

    @GetMapping("/{id}")
    public SubjectResponse getSubjectById(@PathVariable Long id) {
        return subjectService.getSubjectById(id);
    }

    @PutMapping("/{id}")
    public SubjectResponse updateSubject(
            @PathVariable Long id,
            @Valid @RequestBody SubjectRequest request
    ) {
        return subjectService.updateSubject(id, request);
    }

    @PatchMapping("/{id}/status")
    public SubjectResponse updateSubjectStatus(
            @PathVariable Long id,
            @Valid @RequestBody SubjectStatusRequest request
    ) {
        return subjectService.updateSubjectStatus(
                id,
                request.active()
        );
    }
}