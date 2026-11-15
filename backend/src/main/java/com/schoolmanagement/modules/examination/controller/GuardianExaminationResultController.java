package com.schoolmanagement.modules.examination.controller;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import com.schoolmanagement.modules.academic.repository.StudentGuardianRepository;

import com.schoolmanagement.modules.examination.dto.ExaminationReportCardResponse;
import com.schoolmanagement.modules.examination.service.ExaminationResultService;

import com.schoolmanagement.modules.identity.entity.GuardianProfile;
import com.schoolmanagement.modules.identity.repository.GuardianProfileRepository;

@RestController
@RequestMapping("/api/v1/guardian/examinations")
@PreAuthorize("hasRole('GUARDIAN')")
public class GuardianExaminationResultController {

    private final GuardianProfileRepository guardianRepository;
    private final StudentGuardianRepository studentGuardianRepository;
    private final ExaminationResultService resultService;

    public GuardianExaminationResultController(
            GuardianProfileRepository guardianRepository,
            StudentGuardianRepository studentGuardianRepository,
            ExaminationResultService resultService
    ) {
        this.guardianRepository = guardianRepository;
        this.studentGuardianRepository = studentGuardianRepository;
        this.resultService = resultService;
    }

    @GetMapping("/{examId}/students/{studentId}/report-card")
    public ExaminationReportCardResponse getReportCard(
            @PathVariable Long examId,
            @PathVariable Long studentId,
            Authentication authentication
    ) {

        GuardianProfile guardian = guardianRepository
                .findByUserEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Guardian profile not found"
                ));

        boolean linked = studentGuardianRepository
                .existsByStudent_IdAndGuardian_Id(
                        studentId,
                        guardian.getId()
                );

        if (!linked) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Student not found for this guardian"
            );
        }

        // The service also ensures that the examination is PUBLISHED.
        return resultService.getReportCard(examId, studentId);
    }
}