package com.schoolmanagement.modules.examination.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.schoolmanagement.modules.examination.dto.ExaminationReportCardResponse;
import com.schoolmanagement.modules.examination.dto.ExaminationResponse;
import com.schoolmanagement.modules.examination.service.ExaminationResultService;

@RestController
@RequestMapping("/api/v1/examinations")
@PreAuthorize("hasRole('ADMIN')")
public class ExaminationResultController {

    private final ExaminationResultService resultService;

    public ExaminationResultController(
            ExaminationResultService resultService
    ) {
        this.resultService = resultService;
    }

    @PostMapping("/{id}/complete")
    public ExaminationResponse completeExamination(
            @PathVariable Long id
    ) {
        return resultService.completeExamination(id);
    }

    @PostMapping("/{id}/publish")
    public ExaminationResponse publishExamination(
            @PathVariable Long id
    ) {
        return resultService.publishExamination(id);
    }

    @GetMapping("/{examId}/report-cards/{studentId}")
    public ExaminationReportCardResponse getReportCard(
            @PathVariable Long examId,
            @PathVariable Long studentId
    ) {
        return resultService.getReportCard(examId, studentId);
    }
}