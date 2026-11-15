package com.schoolmanagement.modules.examination.dto;

import java.math.BigDecimal;
import java.util.List;

public record ExaminationReportCardResponse(

        Long examinationId,
        String examName,
        String academicYear,

        Long studentId,
        String admissionNumber,
        String studentName,

        Long sectionId,
        String sectionName,
        String className,

        List<ExaminationSubjectResultResponse> subjects,

        BigDecimal totalMarksObtained,
        BigDecimal totalMaximumMarks,
        BigDecimal percentage,

        String grade,
        String gradeRemarks,

        boolean passed

) {
}