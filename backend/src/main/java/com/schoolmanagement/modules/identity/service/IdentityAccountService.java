package com.schoolmanagement.modules.identity.service;

import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.Locale;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.schoolmanagement.common.audit.AuditService;
import com.schoolmanagement.modules.identity.dto.CreateIdentityAccountRequest;
import com.schoolmanagement.modules.identity.dto.IdentityAccountResponse;
import com.schoolmanagement.modules.identity.entity.AppUser;
import com.schoolmanagement.modules.identity.entity.GuardianProfile;
import com.schoolmanagement.modules.identity.entity.TeacherProfile;
import com.schoolmanagement.modules.identity.entity.UserRole;
import com.schoolmanagement.modules.identity.repository.AppUserRepository;
import com.schoolmanagement.modules.identity.repository.GuardianProfileRepository;
import com.schoolmanagement.modules.identity.repository.TeacherProfileRepository;

@Service
public class IdentityAccountService {

    private final AppUserRepository userRepository;
    private final TeacherProfileRepository teacherRepository;
    private final GuardianProfileRepository guardianRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    public IdentityAccountService(
            AppUserRepository userRepository,
            TeacherProfileRepository teacherRepository,
            GuardianProfileRepository guardianRepository,
            PasswordEncoder passwordEncoder,
            AuditService auditService) {
        this.userRepository = userRepository;
        this.teacherRepository = teacherRepository;
        this.guardianRepository = guardianRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
    }

    @Transactional
    public IdentityAccountResponse createAccount(CreateIdentityAccountRequest request) {
        UserRole role = request.role();
        if (role != UserRole.TEACHER && role != UserRole.GUARDIAN) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "This endpoint creates TEACHER or GUARDIAN accounts only");
        }

        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "An account with this email already exists");
        }

        if (request.initialPassword().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Initial password cannot exceed 72 UTF-8 bytes");
        }

        String employeeCode = null;
        if (role == UserRole.TEACHER) {
            if (request.employeeCode() == null || request.employeeCode().isBlank()) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Employee code is required for a teacher account");
            }
            employeeCode = request.employeeCode().trim().toUpperCase(Locale.ROOT);
            if (teacherRepository.existsByEmployeeCodeIgnoreCase(employeeCode)) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Employee code already exists");
            }
        }

        AppUser user = new AppUser();
        user.setFullName(request.fullName().trim());
        user.setEmail(email);
        user.setPhone(normalize(request.phone()));
        user.setPasswordHash(passwordEncoder.encode(request.initialPassword()));
        user.setEnabled(true);
        user.setRoles(Set.of(role));
        AppUser savedUser = userRepository.saveAndFlush(user);

        Long profileId;
        if (role == UserRole.TEACHER) {
            TeacherProfile profile = teacherRepository.saveAndFlush(
                    new TeacherProfile(
                            savedUser.getId(),
                            employeeCode,
                            request.hireDate()));
            profileId = profile.getId();
        } else {
            GuardianProfile profile = guardianRepository.saveAndFlush(
                    new GuardianProfile(
                            savedUser.getId(),
                            normalize(request.alternatePhone())));
            profileId = profile.getId();
        }

        auditService.record(
                "IDENTITY_ACCOUNT_CREATED",
                "APP_USER",
                savedUser.getId().toString(),
                null,
                java.util.Map.of(
                        "email", savedUser.getEmail(),
                        "role", role,
                        "profileId", profileId));

        return new IdentityAccountResponse(
                savedUser.getId(),
                profileId,
                savedUser.getFullName(),
                savedUser.getEmail(),
                role,
                employeeCode);
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
