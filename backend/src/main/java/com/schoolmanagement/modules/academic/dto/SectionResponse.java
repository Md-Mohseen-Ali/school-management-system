package com.schoolmanagement.modules.academic.dto;

public record SectionResponse(
        Long id,
        Long schoolClassId,
        String className,
        String sectionName,
        String academicYear
) {
}