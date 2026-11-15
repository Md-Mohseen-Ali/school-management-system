package com.schoolmanagement.modules.academic.dto;

import jakarta.validation.constraints.NotNull;

public record TeacherAssignmentRequest(

        @NotNull(message = "Teacher ID is required")
        Long teacherId,

        @NotNull(message = "Section ID is required")
        Long sectionId,

        @NotNull(message = "Subject ID is required")
        Long subjectId

) {
}