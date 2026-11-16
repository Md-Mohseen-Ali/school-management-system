package com.schoolmanagement.modules.academic.controller;

import java.time.LocalDate;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.schoolmanagement.modules.academic.dto.StudentEnrollmentRequest;
import com.schoolmanagement.modules.academic.dto.StudentEnrollmentResponse;
import com.schoolmanagement.modules.academic.service.StudentEnrollmentService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/academic/enrollments")
@PreAuthorize("hasRole('ADMIN')")
public class StudentEnrollmentController {

    private final StudentEnrollmentService enrollmentService;

    public StudentEnrollmentController(
            StudentEnrollmentService enrollmentService
    ) {
        this.enrollmentService = enrollmentService;
    }

    @PostMapping
    public StudentEnrollmentResponse createEnrollment(
            @Valid @RequestBody StudentEnrollmentRequest request
    ) {
        return enrollmentService.createEnrollment(request);
    }

    @GetMapping
    public Page<StudentEnrollmentResponse> getAllEnrollments(
            @RequestParam(required = false) Long studentId,
            @RequestParam(required = false) Long sectionId,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) LocalDate attendanceDate,
            @PageableDefault(size = 10, sort = "startDate")
            Pageable pageable
    ) {
        return enrollmentService.getAllEnrollments(
                studentId,
                sectionId,
                active,
                attendanceDate,
                pageable
        );
    }

    @GetMapping("/{id}")
    public StudentEnrollmentResponse getEnrollmentById(
            @PathVariable Long id
    ) {
        return enrollmentService.getEnrollmentById(id);
    }

    @PatchMapping("/{id}/end")
    public StudentEnrollmentResponse endEnrollment(
            @PathVariable Long id
    ) {
        return enrollmentService.endEnrollment(id);
    }
}