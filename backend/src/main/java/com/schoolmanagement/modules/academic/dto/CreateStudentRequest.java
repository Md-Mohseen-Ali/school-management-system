package com.schoolmanagement.modules.academic.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

public record CreateStudentRequest(

        @NotBlank
        @Size(max = 40)
        String admissionNumber,

        @NotBlank
        @Size(max = 80)
        String firstName,

        @NotBlank
        @Size(max = 80)
        String lastName,

        @Past
        LocalDate dateOfBirth,

        @PastOrPresent
        LocalDate admissionDate

) {
}