package com.schoolmanagement.modules.examination.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ExaminationSubjectResponse(

        Long id,
        Long examinationId,

        Long subjectId,
        String subjectCode,
        String subjectName,

        LocalDate examinationDate,

        BigDecimal maxMarks,
        BigDecimal passMarks

) {
}