package com.schoolmanagement.modules.examination.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.transaction.annotation.Transactional;

import com.schoolmanagement.modules.academic.repository.TeacherAssignmentRepository;
import com.schoolmanagement.modules.examination.dto.ExaminationResponse;
import com.schoolmanagement.modules.examination.dto.ExaminationSubjectResponse;
import com.schoolmanagement.modules.examination.entity.Examination;
import com.schoolmanagement.modules.examination.entity.ExaminationSubject;
import com.schoolmanagement.modules.examination.repository.ExaminationRepository;
import com.schoolmanagement.modules.examination.repository.ExaminationSubjectRepository;
import com.schoolmanagement.modules.identity.entity.TeacherProfile;
import com.schoolmanagement.modules.identity.repository.TeacherProfileRepository;

/**
 * Read-only examination workspace for teachers. Only examinations containing
 * a subject assigned to the authenticated teacher are returned. Mark entry is
 * handled by ExaminationMarksController, which performs the same assignment
 * check before writing.
 */
@Transactional(readOnly = true)
@RestController
@RequestMapping("/api/v1/teacher/examinations")
@PreAuthorize("hasRole('TEACHER')")
public class TeacherExaminationController {
    private final ExaminationRepository examinationRepository;
    private final ExaminationSubjectRepository subjectRepository;
    private final TeacherProfileRepository teacherRepository;
    private final TeacherAssignmentRepository assignmentRepository;

    public TeacherExaminationController(
            ExaminationRepository examinationRepository,
            ExaminationSubjectRepository subjectRepository,
            TeacherProfileRepository teacherRepository,
            TeacherAssignmentRepository assignmentRepository
    ) {
        this.examinationRepository = examinationRepository;
        this.subjectRepository = subjectRepository;
        this.teacherRepository = teacherRepository;
        this.assignmentRepository = assignmentRepository;
    }

    @GetMapping
    public List<ExaminationResponse> getMyExaminations(
            Authentication authentication
    ) {
        TeacherProfile teacher = getTeacher(authentication.getName());

        return examinationRepository.findAll().stream()
                .filter(exam -> subjectsForTeacher(exam, teacher).size() > 0)
                .map(this::toResponse)
                .toList();
    }

    @GetMapping("/{examId}/subjects")
    public List<ExaminationSubjectResponse> getMyExaminationSubjects(
            @PathVariable Long examId,
            Authentication authentication
    ) {
        TeacherProfile teacher = getTeacher(authentication.getName());

        Examination exam = examinationRepository.findById(examId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Examination not found"
                ));

        List<ExaminationSubject> subjects = subjectsForTeacher(exam, teacher);
        if (subjects.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "No assigned subjects were found for this examination"
            );
        }

        return subjects.stream().map(this::toSubjectResponse).toList();
    }

    private TeacherProfile getTeacher(String email) {
        return teacherRepository.findActiveByUserEmail(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Active teacher profile not found"
                ));
    }

    private List<ExaminationSubject> subjectsForTeacher(
            Examination examination,
            TeacherProfile teacher
    ) {
        return subjectRepository
                .findByExamination_IdOrderByExaminationDateAsc(examination.getId())
                .stream()
                .filter(subject -> assignmentRepository
                        .existsByTeacher_IdAndSection_IdAndSubject_IdAndActiveTrue(
                                teacher.getId(),
                                examination.getSection().getId(),
                                subject.getSubject().getId()
                        ))
                .toList();
    }

    private ExaminationResponse toResponse(Examination exam) {
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
    }

    private ExaminationSubjectResponse toSubjectResponse(ExaminationSubject subject) {
        var value = subject.getSubject();
        return new ExaminationSubjectResponse(
                subject.getId(),
                subject.getExamination().getId(),
                value.getId(),
                value.getSubjectCode(),
                value.getName(),
                subject.getExaminationDate(),
                subject.getMaxMarks(),
                subject.getPassMarks()
        );
    }
}
