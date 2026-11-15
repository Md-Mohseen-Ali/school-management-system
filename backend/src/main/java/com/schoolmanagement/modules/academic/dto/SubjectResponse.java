package com.schoolmanagement.modules.academic.dto;

public record SubjectResponse(
        Long id,
        String subjectCode,
        String name,
        boolean active
) {
}