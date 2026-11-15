package com.schoolmanagement.modules.academic.dto;

import java.time.LocalDate;

public record TeacherAssignmentResponse(

        Long id,

        Long teacherId,
        String employeeCode,

        Long sectionId,
        String sectionName,
        String className,
        String academicYear,

        Long subjectId,
        String subjectCode,
        String subjectName,

        LocalDate assignedOn,
        boolean active

) {
}