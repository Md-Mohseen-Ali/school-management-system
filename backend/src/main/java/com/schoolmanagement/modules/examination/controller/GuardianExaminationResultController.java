package com.schoolmanagement.modules.examination.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import com.schoolmanagement.modules.academic.repository.StudentGuardianRepository;
import com.schoolmanagement.modules.academic.repository.StudentEnrollmentRepository;
import com.schoolmanagement.modules.examination.dto.ExaminationResponse;
import com.schoolmanagement.modules.examination.entity.ExaminationStatus;
import com.schoolmanagement.modules.examination.repository.ExaminationRepository;

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
    private final StudentEnrollmentRepository enrollmentRepository;
    private final ExaminationRepository examinationRepository;
    private final ExaminationResultService resultService;

    public GuardianExaminationResultController(
            GuardianProfileRepository guardianRepository,
            StudentGuardianRepository studentGuardianRepository,
            StudentEnrollmentRepository enrollmentRepository,
            ExaminationRepository examinationRepository,
            ExaminationResultService resultService
    ) {
        this.guardianRepository = guardianRepository;
        this.studentGuardianRepository = studentGuardianRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.examinationRepository = examinationRepository;
        this.resultService = resultService;
    }

    @GetMapping
    public List<ExaminationResponse> getPublishedExaminations(
            Authentication authentication
    ) {
        GuardianProfile guardian = guardianRepository
                .findByUserEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Guardian profile not found"
                ));

        List<Long> studentIds = studentGuardianRepository
                .findByGuardian_Id(guardian.getId())
                .stream()
                .map(link -> link.getStudent().getId())
                .toList();

        if (studentIds.isEmpty()) {
            return List.of();
        }

        List<Long> sectionIds = enrollmentRepository
                .findByStudent_IdInAndEndDateIsNull(studentIds)
                .stream()
                .map(enrollment -> enrollment.getSection().getId())
                .distinct()
                .toList();

        if (sectionIds.isEmpty()) {
            return List.of();
        }

        return examinationRepository
                .findBySection_IdInAndStatusOrderByStartDateDesc(
                        sectionIds,
                        ExaminationStatus.PUBLISHED
                )
                .stream()
                .map(exam -> {
                    var section = exam.getSection();
                    return new ExaminationResponse(
                            exam.getId(),
                            section.getId(),
                            section.getSectionName(),
                            section.getSchoolClass().getName(),
                            exam.getExamName(),
                            exam.getAcademicYear(),
                            exam.getStartDate(),
                            exam.getEndDate(),
                            exam.getStatus(),
                            exam.getCreatedAt()
                    );
                })
                .toList();
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