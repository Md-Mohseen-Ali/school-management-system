package com.schoolmanagement.modules.academic.dto;

import java.time.LocalDate;

public record StudentSubjectTeacherResponse(
        Long subjectId,
        String subjectCode,
        String subjectName,
        String teacherName,
        String teacherEmail,
        LocalDate assignedOn
) {
}
