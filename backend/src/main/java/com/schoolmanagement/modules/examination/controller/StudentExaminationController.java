package com.schoolmanagement.modules.examination.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import com.schoolmanagement.modules.academic.entity.Student;
import com.schoolmanagement.modules.academic.repository.StudentEnrollmentRepository;
import com.schoolmanagement.modules.academic.repository.StudentRepository;
import com.schoolmanagement.modules.examination.dto.ExaminationReportCardResponse;
import com.schoolmanagement.modules.examination.dto.ExaminationResponse;
import com.schoolmanagement.modules.examination.entity.ExaminationStatus;
import com.schoolmanagement.modules.examination.repository.ExaminationRepository;
import com.schoolmanagement.modules.examination.service.ExaminationResultService;

/**
 * Student self-service portal. Student identity is resolved from the JWT
 * principal; callers cannot choose another student ID.
 */
@RestController
@RequestMapping("/api/v1/student/examinations")
@PreAuthorize("hasRole('STUDENT')")
public class StudentExaminationController {

    private final StudentRepository studentRepository;
    private final StudentEnrollmentRepository enrollmentRepository;
    private final ExaminationRepository examinationRepository;
    private final ExaminationResultService resultService;

    public StudentExaminationController(
            StudentRepository studentRepository,
            StudentEnrollmentRepository enrollmentRepository,
            ExaminationRepository examinationRepository,
            ExaminationResultService resultService
    ) {
        this.studentRepository = studentRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.examinationRepository = examinationRepository;
        this.resultService = resultService;
    }

    @GetMapping
    public List<ExaminationResponse> getMyPublishedExaminations(
            Authentication authentication
    ) {
        Student student = getStudent(authentication.getName());

        List<Long> sectionIds = enrollmentRepository
                .findByStudent_IdInAndEndDateIsNull(List.of(student.getId()))
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

    @GetMapping("/{examId}/report-card")
    public ExaminationReportCardResponse getMyReportCard(
            @PathVariable Long examId,
            Authentication authentication
    ) {
        Student student = getStudent(authentication.getName());
        // The result service independently verifies enrollment on each subject date.
        return resultService.getReportCard(examId, student.getId());
    }

    private Student getStudent(String email) {
        return studentRepository.findByUser_EmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Student profile not found for this account"
                ));
    }
}
