package com.schoolmanagement.modules.examination.dto;

import java.math.BigDecimal;
import java.time.Instant;

import com.schoolmanagement.modules.examination.entity.ExamAttendanceStatus;

public record ExaminationMarkResponse(

        Long id,

        Long examinationSubjectId,

        Long studentId,
        String admissionNumber,
        String studentName,

        BigDecimal marksObtained,
        BigDecimal maxMarks,
        BigDecimal passMarks,

        ExamAttendanceStatus attendanceStatus,
        boolean passed,

        String remarks,

        Instant enteredAt,
        Instant updatedAt

) {
}