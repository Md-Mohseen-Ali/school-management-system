package com.schoolmanagement.modules.academic.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

import com.schoolmanagement.modules.academic.dto.SchoolClassRequest;
import com.schoolmanagement.modules.academic.dto.SchoolClassResponse;
import com.schoolmanagement.modules.academic.service.SchoolClassService;

import jakarta.validation.Valid;

import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/api/v1/academic/classes")
@PreAuthorize("hasRole('ADMIN')")
public class SchoolClassController {

    private final SchoolClassService schoolClassService;

    public SchoolClassController(SchoolClassService schoolClassService) {
        this.schoolClassService = schoolClassService;
    }

    @PostMapping
    public SchoolClassResponse createClass(
            @Valid @RequestBody SchoolClassRequest request
    ) {
        return schoolClassService.createClass(request);
    }

    @GetMapping
    public Page<SchoolClassResponse> getAllClasses(
            @RequestParam(required = false) String search,
            @PageableDefault(size = 10, sort = "name") Pageable pageable
    ) {
        return schoolClassService.getAllClasses(search, pageable);
    }

    @GetMapping("/{id}")
    public SchoolClassResponse getClassById(@PathVariable Long id) {
        return schoolClassService.getClassById(id);
    }

    @PutMapping("/{id}")
    public SchoolClassResponse updateClass(
            @PathVariable Long id,
            @Valid @RequestBody SchoolClassRequest request
    ) {
        return schoolClassService.updateClass(id, request);
    }
}