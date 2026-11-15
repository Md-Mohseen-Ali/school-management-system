package com.schoolmanagement.modules.examination.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.schoolmanagement.modules.examination.entity.ExamAttendanceStatus;

public record ExaminationSubjectResultResponse(

        Long examinationSubjectId,
        Long subjectId,
        String subjectCode,
        String subjectName,

        LocalDate examinationDate,

        BigDecimal marksObtained,
        BigDecimal maxMarks,
        BigDecimal passMarks,

        ExamAttendanceStatus attendanceStatus,
        boolean passed

) {
}