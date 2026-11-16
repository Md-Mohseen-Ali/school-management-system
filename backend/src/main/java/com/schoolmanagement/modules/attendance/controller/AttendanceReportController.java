package com.schoolmanagement.modules.attendance.controller;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import com.schoolmanagement.modules.attendance.dto.AttendanceSectionSummaryResponse;
import com.schoolmanagement.modules.attendance.dto.AttendanceStudentSummaryResponse;
import com.schoolmanagement.modules.attendance.service.AttendanceReportService;

import com.schoolmanagement.modules.identity.entity.TeacherProfile;
import com.schoolmanagement.modules.identity.repository.TeacherProfileRepository;

import com.schoolmanagement.modules.academic.repository.TeacherAssignmentRepository;
import com.schoolmanagement.modules.academic.repository.StudentEnrollmentRepository;

@RestController
@RequestMapping("/api/v1/attendance/reports")
@PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
public class AttendanceReportController {

    private final AttendanceReportService reportService;
    private final TeacherProfileRepository teacherRepository;
    private final TeacherAssignmentRepository assignmentRepository;
    private final StudentEnrollmentRepository enrollmentRepository;

    public AttendanceReportController(
            AttendanceReportService reportService,
            TeacherProfileRepository teacherRepository,
            TeacherAssignmentRepository assignmentRepository,
            StudentEnrollmentRepository enrollmentRepository
    ) {
        this.reportService = reportService;
        this.teacherRepository = teacherRepository;
        this.assignmentRepository = assignmentRepository;
        this.enrollmentRepository = enrollmentRepository;
    }

    private boolean isAdmin(Authentication authentication) {

        return authentication.getAuthorities().stream()
                .anyMatch(authority ->
                        authority.getAuthority().equals("ROLE_ADMIN"));
    }

    private void validateDateRange(LocalDate fromDate, LocalDate toDate) {
        if (fromDate == null || toDate == null || toDate.isBefore(fromDate)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Provide a valid date range; toDate cannot be before fromDate");
        }
    }

    private void verifyTeacherSection(
            String email,
            Long sectionId
    ) {

        TeacherProfile teacher = teacherRepository
                .findActiveByUserEmail(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "No active teacher profile found"
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
    }

    @GetMapping("/students/{studentId}")
    public AttendanceStudentSummaryResponse getStudentReport(
            @PathVariable Long studentId,

            @RequestParam(required = false) Long sectionId,

            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fromDate,

            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate toDate,

            Authentication authentication
    ) {

        if (!isAdmin(authentication)) {

            if (sectionId == null) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Teachers must specify sectionId"
                );
            }

            validateDateRange(fromDate, toDate);
            verifyTeacherSection(
                    authentication.getName(),
                    sectionId
            );
            boolean enrolledInRequestedSection = enrollmentRepository
                    .existsEnrollmentInSectionDuringRange(
                            studentId, sectionId, fromDate, toDate);
            if (!enrolledInRequestedSection) {
                // Deliberately hide whether an unrelated student ID exists.
                throw new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Student report not found for this section"
                );
            }
        }

        return reportService.getStudentSummary(
                studentId,
                sectionId,
                fromDate,
                toDate
        );
    }

    @GetMapping("/sections/{sectionId}")
    public AttendanceSectionSummaryResponse getSectionReport(
            @PathVariable Long sectionId,

            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fromDate,

            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate toDate,

            Authentication authentication
    ) {

        if (!isAdmin(authentication)) {
            verifyTeacherSection(
                    authentication.getName(),
                    sectionId
            );
        }

        return reportService.getSectionSummary(
                sectionId,
                fromDate,
                toDate
        );
    }
}