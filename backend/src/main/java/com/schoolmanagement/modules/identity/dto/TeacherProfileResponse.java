package com.schoolmanagement.modules.identity.dto;

import java.time.LocalDate;

public record TeacherProfileResponse(
        Long id,
        Long userId,
        String fullName,
        String email,
        String phone,
        String employeeCode,
        LocalDate hireDate,
        boolean active
) {
}
