package com.schoolmanagement.modules.academic.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.schoolmanagement.modules.academic.dto.AssignmentStatusRequest;
import com.schoolmanagement.modules.academic.dto.TeacherAssignmentRequest;
import com.schoolmanagement.modules.academic.dto.TeacherAssignmentResponse;
import com.schoolmanagement.modules.academic.service.TeacherAssignmentService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/academic/teacher-assignments")
@PreAuthorize("hasRole('ADMIN')")
public class TeacherAssignmentController {

    private final TeacherAssignmentService assignmentService;

    public TeacherAssignmentController(
            TeacherAssignmentService assignmentService
    ) {
        this.assignmentService = assignmentService;
    }

    @PostMapping
    public TeacherAssignmentResponse createAssignment(
            @Valid @RequestBody TeacherAssignmentRequest request
    ) {
        return assignmentService.createAssignment(request);
    }

    @GetMapping
    public Page<TeacherAssignmentResponse> getAllAssignments(
            @RequestParam(required = false) Long teacherId,
            @RequestParam(required = false) Long sectionId,
            @RequestParam(required = false) Long subjectId,
            @RequestParam(required = false) Boolean active,
            @PageableDefault(size = 10, sort = "assignedOn")
            Pageable pageable
    ) {
        return assignmentService.getAllAssignments(
                teacherId,
                sectionId,
                subjectId,
                active,
                pageable
        );
    }

    @GetMapping("/{id}")
    public TeacherAssignmentResponse getAssignmentById(
            @PathVariable Long id
    ) {
        return assignmentService.getAssignmentById(id);
    }

    @PatchMapping("/{id}/status")
    public TeacherAssignmentResponse updateAssignmentStatus(
            @PathVariable Long id,
            @Valid @RequestBody AssignmentStatusRequest request
    ) {
        return assignmentService.updateAssignmentStatus(
                id,
                request.active()
        );
    }
}