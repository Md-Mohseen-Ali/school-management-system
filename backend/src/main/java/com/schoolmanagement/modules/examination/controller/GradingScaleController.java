package com.schoolmanagement.modules.examination.controller;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.schoolmanagement.modules.examination.dto.GradingScaleRequest;
import com.schoolmanagement.modules.examination.dto.GradingScaleResponse;
import com.schoolmanagement.modules.examination.service.GradingScaleService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/examinations/grading-scales")
@PreAuthorize("hasRole('ADMIN')")
public class GradingScaleController {

    private final GradingScaleService gradingScaleService;

    public GradingScaleController(
            GradingScaleService gradingScaleService
    ) {
        this.gradingScaleService = gradingScaleService;
    }

    @PostMapping
    public GradingScaleResponse createGrade(
            @Valid @RequestBody GradingScaleRequest request
    ) {
        return gradingScaleService.createGrade(request);
    }

    @GetMapping
    public List<GradingScaleResponse> getGrades(
            @RequestParam String academicYear
    ) {
        return gradingScaleService.getGrades(academicYear);
    }
}