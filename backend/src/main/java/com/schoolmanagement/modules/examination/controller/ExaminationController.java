package com.schoolmanagement.modules.examination.controller;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.schoolmanagement.modules.examination.dto.ExaminationRequest;
import com.schoolmanagement.modules.examination.dto.ExaminationResponse;
import com.schoolmanagement.modules.examination.dto.ExaminationSubjectRequest;
import com.schoolmanagement.modules.examination.dto.ExaminationSubjectResponse;
import com.schoolmanagement.modules.examination.service.ExaminationService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/examinations")
@PreAuthorize("hasRole('ADMIN')")
public class ExaminationController {

    private final ExaminationService examinationService;

    public ExaminationController(ExaminationService examinationService) {
        this.examinationService = examinationService;
    }

    @PostMapping
    public ExaminationResponse createExamination(
            @Valid @RequestBody ExaminationRequest request
    ) {
        return examinationService.createExamination(request);
    }

    @GetMapping
    public Page<ExaminationResponse> getAllExaminations(
            @RequestParam(required = false) Long sectionId,
            @RequestParam(required = false) String academicYear,
            @RequestParam(required = false) 
            com.schoolmanagement.modules.examination.entity.ExaminationStatus status,
            @PageableDefault(size = 10, sort = "startDate")
            Pageable pageable
    ) {
        return examinationService.getAllExaminations(
                sectionId,
                academicYear,
                status,
                pageable
        );
    }

    @GetMapping("/{id}")
    public ExaminationResponse getExamination(@PathVariable Long id) {
        return examinationService.getExamination(id);
    }

    @PostMapping("/{id}/subjects")
    public ExaminationSubjectResponse addExaminationSubject(
            @PathVariable Long id,
            @Valid @RequestBody ExaminationSubjectRequest request
    ) {
        return examinationService.addExaminationSubject(id, request);
    }

    @GetMapping("/{id}/subjects")
    public List<ExaminationSubjectResponse> getExaminationSubjects(
            @PathVariable Long id
    ) {
        return examinationService.getExaminationSubjects(id);
    }

    @PostMapping("/{id}/schedule")
    public ExaminationResponse scheduleExamination(
            @PathVariable Long id
    ) {
        return examinationService.scheduleExamination(id);
    }
}