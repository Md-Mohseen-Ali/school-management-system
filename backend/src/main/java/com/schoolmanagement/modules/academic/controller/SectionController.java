package com.schoolmanagement.modules.academic.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.schoolmanagement.modules.academic.dto.SectionRequest;
import com.schoolmanagement.modules.academic.dto.SectionResponse;
import com.schoolmanagement.modules.academic.service.SectionService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/academic/sections")
@PreAuthorize("hasRole('ADMIN')")
public class SectionController {

    private final SectionService sectionService;

    public SectionController(SectionService sectionService) {
        this.sectionService = sectionService;
    }

    @PostMapping
    public SectionResponse createSection(
            @Valid @RequestBody SectionRequest request
    ) {
        return sectionService.createSection(request);
    }

    @GetMapping
    public Page<SectionResponse> getAllSections(
            @RequestParam(required = false) Long classId,
            @RequestParam(required = false) String academicYear,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 10, sort = "sectionName")
            Pageable pageable
    ) {
        return sectionService.getAllSections(
                classId,
                academicYear,
                search,
                pageable
        );
    }

    @GetMapping("/{id}")
    public SectionResponse getSectionById(@PathVariable Long id) {
        return sectionService.getSectionById(id);
    }

    @PutMapping("/{id}")
    public SectionResponse updateSection(
            @PathVariable Long id,
            @Valid @RequestBody SectionRequest request
    ) {
        return sectionService.updateSection(id, request);
    }
}