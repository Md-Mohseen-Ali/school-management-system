package com.schoolmanagement.modules.examination.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import com.schoolmanagement.common.audit.AuditService;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.schoolmanagement.modules.examination.dto.GradingScaleRequest;
import com.schoolmanagement.modules.examination.dto.GradingScaleResponse;
import com.schoolmanagement.modules.examination.entity.GradingScale;
import com.schoolmanagement.modules.examination.repository.GradingScaleRepository;

@Service
public class GradingScaleService {

    private final GradingScaleRepository gradingScaleRepository;
    private final AuditService auditService;

    public GradingScaleService(
            GradingScaleRepository gradingScaleRepository,
            AuditService auditService
    ) {
        this.gradingScaleRepository = gradingScaleRepository;
        this.auditService = auditService;
    }

    @Transactional
    public GradingScaleResponse createGrade(GradingScaleRequest request) {

        String academicYear = request.academicYear().trim();
        String grade = request.grade().trim().toUpperCase();

        BigDecimal min = request.minPercentage();
        BigDecimal max = request.maxPercentage();

        if (min.compareTo(max) > 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Minimum percentage cannot exceed maximum percentage"
            );
        }

        if (gradingScaleRepository.existsByAcademicYearAndGrade(
                academicYear, grade)) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "This grade already exists for the academic year"
            );
        }

        List<GradingScale> existingGrades =
                gradingScaleRepository
                        .findByAcademicYearOrderByMinPercentageDesc(
                                academicYear
                        );

        boolean overlaps = existingGrades.stream().anyMatch(existing ->
                min.compareTo(existing.getMaxPercentage()) <= 0
                && max.compareTo(existing.getMinPercentage()) >= 0
        );

        if (overlaps) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Grading percentage range overlaps an existing grade"
            );
        }

        GradingScale gradingScale = new GradingScale(
                academicYear,
                grade,
                min,
                max,
                request.remarks() == null
                        ? null
                        : request.remarks().trim()
        );

        GradingScale saved = gradingScaleRepository.save(gradingScale);
        auditService.record("GRADING_SCALE_CREATED", "GRADING_SCALE", saved.getId().toString(),
                null, Map.of("academicYear", saved.getAcademicYear(),
                        "grade", saved.getGrade(),
                        "minPercentage", saved.getMinPercentage(),
                        "maxPercentage", saved.getMaxPercentage()));
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<GradingScaleResponse> getGrades(String academicYear) {

        if (academicYear == null || academicYear.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Academic year is required"
            );
        }

        return gradingScaleRepository
                .findByAcademicYearOrderByMinPercentageDesc(
                        academicYear.trim()
                )
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private GradingScaleResponse toResponse(GradingScale grade) {

        return new GradingScaleResponse(
                grade.getId(),
                grade.getAcademicYear(),
                grade.getGrade(),
                grade.getMinPercentage(),
                grade.getMaxPercentage(),
                grade.getRemarks()
        );
    }
}