package com.schoolmanagement.modules.academic.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.transaction.annotation.Transactional;

import com.schoolmanagement.modules.academic.dto.StudentResponse;
import com.schoolmanagement.modules.academic.entity.StudentGuardian;
import com.schoolmanagement.modules.academic.repository.StudentGuardianRepository;
import com.schoolmanagement.modules.identity.entity.GuardianProfile;
import com.schoolmanagement.modules.identity.repository.GuardianProfileRepository;

/**
 * Returns only students explicitly linked to the authenticated guardian.
 */
@RestController
@RequestMapping("/api/v1/guardian/students")
@PreAuthorize("hasRole('GUARDIAN')")
public class GuardianStudentController {
    @Transactional(readOnly = true)

    private final GuardianProfileRepository guardianRepository;
    private final StudentGuardianRepository studentGuardianRepository;

    public GuardianStudentController(
            GuardianProfileRepository guardianRepository,
            StudentGuardianRepository studentGuardianRepository
    ) {
        this.guardianRepository = guardianRepository;
        this.studentGuardianRepository = studentGuardianRepository;
    }

    @GetMapping
    public List<StudentResponse> getLinkedStudents(Authentication authentication) {
        GuardianProfile guardian = guardianRepository
                .findByUserEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Guardian profile not found"
                ));

        return studentGuardianRepository.findByGuardian_Id(guardian.getId())
                .stream()
                .map(StudentGuardian::getStudent)
                .map(student -> new StudentResponse(
                        student.getId(),
                        student.getAdmissionNumber(),
                        student.getFirstName(),
                        student.getLastName(),
                        student.getDateOfBirth(),
                        student.getAdmissionDate(),
                        student.getStatus()
                ))
                .toList();
    }
}
