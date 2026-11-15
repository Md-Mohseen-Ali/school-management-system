package com.schoolmanagement.modules.academic.dto;

public record StudentGuardianResponse(

        Long studentId,
        String admissionNumber,
        String studentName,

        Long guardianId,
        Long guardianUserId,
        String alternatePhone,

        String relationship,
        boolean primary

) {
}