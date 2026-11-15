package com.schoolmanagement.modules.academic.dto;

import java.time.LocalDate;

import com.schoolmanagement.modules.academic.entity.StudentStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

public record UpdateStudentRequest(

        @NotBlank
        @Size(max = 80)
        String firstName,

        @NotBlank
        @Size(max = 80)
        String lastName,

        @Past
        LocalDate dateOfBirth,

        @NotNull
        StudentStatus status

) {
}