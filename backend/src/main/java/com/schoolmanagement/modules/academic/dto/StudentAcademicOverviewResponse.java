package com.schoolmanagement.modules.academic.dto;

import java.util.List;

public record StudentAcademicOverviewResponse(
        StudentPortalProfileResponse student,
        List<StudentSubjectTeacherResponse> subjects
) {
}
