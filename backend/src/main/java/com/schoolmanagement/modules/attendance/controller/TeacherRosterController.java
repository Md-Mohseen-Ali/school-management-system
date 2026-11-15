package com.schoolmanagement.modules.attendance.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.transaction.annotation.Transactional;

import com.schoolmanagement.modules.academic.dto.StudentEnrollmentResponse;
import com.schoolmanagement.modules.academic.entity.StudentEnrollment;
import com.schoolmanagement.modules.academic.entity.TeacherAssignment;
import com.schoolmanagement.modules.academic.repository.StudentEnrollmentRepository;
import com.schoolmanagement.modules.academic.repository.TeacherAssignmentRepository;
import com.schoolmanagement.modules.identity.entity.TeacherProfile;
import com.schoolmanagement.modules.identity.repository.TeacherProfileRepository;

/**
 * Narrow teacher roster endpoint. Unlike the administrator enrollment
 * directory, this only returns students in a section assigned to the
 * currently authenticated teacher for the requested attendance date.
 */
@RestController
@RequestMapping("/api/v1/teacher/roster")
@PreAuthorize("hasRole('TEACHER')")
public class TeacherRosterController {
    @Transactional(readOnly = true)

    private final TeacherProfileRepository teacherRepository;
    private final TeacherAssignmentRepository assignmentRepository;
    private final StudentEnrollmentRepository enrollmentRepository;

    public TeacherRosterController(
            TeacherProfileRepository teacherRepository,
            TeacherAssignmentRepository assignmentRepository,
            StudentEnrollmentRepository enrollmentRepository
    ) {
        this.teacherRepository = teacherRepository;
        this.assignmentRepository = assignmentRepository;
        this.enrollmentRepository = enrollmentRepository;
    }

    public record SectionOption(
            Long id,
            String sectionName,
            String className,
            String academicYear
    ) {}

    @GetMapping("/sections")
    public List<SectionOption> getAssignedSections(Authentication authentication) {
        TeacherProfile teacher = teacherRepository
                .findActiveByUserEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Active teacher profile not found"
                ));

        Map<Long, SectionOption> unique = new LinkedHashMap<>();
        for (TeacherAssignment assignment :
                assignmentRepository.findByTeacher_IdAndActiveTrue(teacher.getId())) {
            var section = assignment.getSection();
            unique.putIfAbsent(
                    section.getId(),
                    new SectionOption(
                            section.getId(),
                            section.getSectionName(),
                            section.getSchoolClass().getName(),
                            section.getAcademicYear()
                    )
            );
        }
        return List.copyOf(unique.values());
    }

    @GetMapping
    public List<StudentEnrollmentResponse> getRoster(
            @RequestParam Long sectionId,
            @RequestParam LocalDate attendanceDate,
            Authentication authentication
    ) {
        TeacherProfile teacher = teacherRepository
                .findActiveByUserEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Active teacher profile not found"
                ));

        boolean assigned = assignmentRepository
                .existsByTeacher_IdAndSection_IdAndActiveTrue(
                        teacher.getId(),
                        sectionId
                );

        if (!assigned) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You are not assigned to this section"
            );
        }

        return enrollmentRepository
                .findEnrollmentsOnDate(sectionId, attendanceDate)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private StudentEnrollmentResponse toResponse(StudentEnrollment enrollment) {
        var student = enrollment.getStudent();
        var section = enrollment.getSection();
        return new StudentEnrollmentResponse(
                enrollment.getId(),
                student.getId(),
                student.getAdmissionNumber(),
                student.getFirstName() + " " + student.getLastName(),
                section.getId(),
                section.getSectionName(),
                section.getSchoolClass().getName(),
                section.getAcademicYear(),
                enrollment.getStartDate(),
                enrollment.getEndDate(),
                enrollment.isActive()
        );
    }
}
