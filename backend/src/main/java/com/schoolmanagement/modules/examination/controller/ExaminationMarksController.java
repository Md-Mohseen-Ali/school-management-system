package com.schoolmanagement.modules.examination.controller;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.schoolmanagement.modules.examination.dto.ExaminationMarkResponse;
import com.schoolmanagement.modules.examination.dto.ExaminationMarksRequest;
import com.schoolmanagement.modules.examination.service.ExaminationMarksService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/examinations/subjects")
@PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
public class ExaminationMarksController {

    private final ExaminationMarksService marksService;

    public ExaminationMarksController(
            ExaminationMarksService marksService
    ) {
        this.marksService = marksService;
    }

    private boolean isAdmin(Authentication authentication) {

        return authentication.getAuthorities().stream()
                .anyMatch(authority ->
                        authority.getAuthority().equals("ROLE_ADMIN"));
    }

    @PutMapping("/{examinationSubjectId}/marks")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public List<ExaminationMarkResponse> saveMarks(
            @PathVariable Long examinationSubjectId,
            @Valid @RequestBody ExaminationMarksRequest request,
            Authentication authentication
    ) {
        return marksService.saveMarks(
                examinationSubjectId,
                request,
                authentication.getName(),
                isAdmin(authentication)
        );
    }

    @GetMapping("/{examinationSubjectId}/marks")
    public List<ExaminationMarkResponse> getMarks(
            @PathVariable Long examinationSubjectId,
            Authentication authentication
    ) {
        return marksService.getMarksForSubject(
                examinationSubjectId,
                authentication.getName(),
                isAdmin(authentication)
        );
    }
}