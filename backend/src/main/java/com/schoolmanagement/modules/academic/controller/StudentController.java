package com.schoolmanagement.modules.academic.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.schoolmanagement.modules.academic.dto.CreateStudentRequest;
import com.schoolmanagement.modules.academic.dto.StudentResponse;
import com.schoolmanagement.modules.academic.dto.UpdateStudentRequest;
import com.schoolmanagement.modules.academic.entity.StudentStatus;
import com.schoolmanagement.modules.academic.service.StudentService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/academic/students")
@PreAuthorize("hasRole('ADMIN')")
public class StudentController {

    private final StudentService studentService;

    public StudentController(StudentService studentService) {
        this.studentService = studentService;
    }

    // Register student
    @PostMapping
    public ResponseEntity<StudentResponse> createStudent(
            @Valid @RequestBody CreateStudentRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(studentService.createStudent(request));
    }

    // Search and list students
    @GetMapping
    public Page<StudentResponse> getAllStudents(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) StudentStatus status,
            @PageableDefault(
                    size = 10,
                    sort = "admissionNumber"
            ) Pageable pageable) {

        return studentService.getAllStudents(
                search, status, pageable);
    }

    // Get student by ID
    @GetMapping("/{id}")
    public StudentResponse getStudentById(@PathVariable Long id) {

        return studentService.getStudentById(id);
    }

    // Update student
    @PutMapping("/{id}")
    public StudentResponse updateStudent(
            @PathVariable Long id,
            @Valid @RequestBody UpdateStudentRequest request) {

        return studentService.updateStudent(id, request);
    }

    // Deactivate student
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivateStudent(
            @PathVariable Long id) {

        studentService.deactivateStudent(id);

        return ResponseEntity.noContent().build();
    }
}