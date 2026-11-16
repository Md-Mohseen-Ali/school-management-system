package com.schoolmanagement.modules.identity.service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.schoolmanagement.common.audit.AuditService;
import com.schoolmanagement.modules.academic.dto.CreateStudentRequest;
import com.schoolmanagement.modules.academic.dto.StudentGuardianRequest;
import com.schoolmanagement.modules.academic.entity.Student;
import com.schoolmanagement.modules.academic.repository.StudentRepository;
import com.schoolmanagement.modules.academic.service.StudentGuardianService;
import com.schoolmanagement.modules.academic.service.StudentService;
import com.schoolmanagement.modules.identity.dto.*;
import com.schoolmanagement.modules.identity.entity.*;
import com.schoolmanagement.modules.identity.repository.AccountAccessRequestRepository;
import com.schoolmanagement.modules.identity.repository.AppUserRepository;
import com.schoolmanagement.modules.identity.repository.GuardianProfileRepository;

@Service
public class AccountAccessRequestService {

    private static final ZoneId SCHOOL_ZONE = ZoneId.of("Asia/Kolkata");

    private final AccountAccessRequestRepository requestRepository;
    private final AppUserRepository userRepository;
    private final StudentRepository studentRepository;
    private final GuardianProfileRepository guardianRepository;
    private final IdentityAccountService identityAccountService;
    private final StudentService studentService;
    private final StudentGuardianService studentGuardianService;
    private final AuditService auditService;

    public AccountAccessRequestService(
            AccountAccessRequestRepository requestRepository,
            AppUserRepository userRepository,
            StudentRepository studentRepository,
            GuardianProfileRepository guardianRepository,
            IdentityAccountService identityAccountService,
            StudentService studentService,
            StudentGuardianService studentGuardianService,
            AuditService auditService) {
        this.requestRepository = requestRepository;
        this.userRepository = userRepository;
        this.studentRepository = studentRepository;
        this.guardianRepository = guardianRepository;
        this.identityAccountService = identityAccountService;
        this.studentService = studentService;
        this.studentGuardianService = studentGuardianService;
        this.auditService = auditService;
    }

    @Transactional
    public AccountAccessRequestSubmissionResponse submit(CreateAccountAccessRequest input) {
        UserRole role = input.requestedRole();
        if (role == UserRole.ADMIN) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Administrator accounts cannot be requested through this form");
        }

        String email = input.email().trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "An account already exists for this email address");
        }
        if (requestRepository.existsByEmailIgnoreCaseAndRequestedRoleAndStatus(
                email, role, AccountRequestStatus.PENDING)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A pending request already exists for this email and account type");
        }

        validateRoleFields(input, role);

        AccountAccessRequest request = new AccountAccessRequest();
        request.setRequestedRole(role);
        request.setFullName(input.fullName().trim());
        request.setEmail(email);
        request.setPhone(normalize(input.phone()));
        request.setEmployeeCode(normalize(input.employeeCode()));
        request.setHireDate(input.hireDate());
        request.setAlternatePhone(normalize(input.alternatePhone()));
        request.setAdmissionNumber(normalize(input.admissionNumber()));
        request.setFirstName(normalize(input.firstName()));
        request.setLastName(normalize(input.lastName()));
        request.setDateOfBirth(input.dateOfBirth());
        request.setStudentAdmissionNumber(normalize(input.studentAdmissionNumber()));
        request.setRelationship(normalize(input.relationship()));

        AccountAccessRequest saved = requestRepository.saveAndFlush(request);
        return new AccountAccessRequestSubmissionResponse(
                saved.getId(),
                saved.getStatus(),
                saved.getCreatedAt(),
                "Your request was submitted. The school administrator must verify and approve it before an account is created."
        );
    }

    @Transactional(readOnly = true)
    public List<AccountAccessRequestResponse> getRequests(AccountRequestStatus status) {
        List<AccountAccessRequest> requests = status == null
                ? requestRepository.findAllByOrderByCreatedAtDesc()
                : requestRepository.findByStatusOrderByCreatedAtDesc(status);
        return requests.stream().map(this::toResponse).toList();
    }

    @Transactional
    public AccountAccessRequestResponse approve(
            Long requestId, ApproveAccountAccessRequest review, String adminEmail) {
        AccountAccessRequest request = requestRepository.findByIdForUpdate(requestId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Account access request not found"));
        ensurePending(request);

        String password = review.initialPassword();
        if (password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Initial password cannot exceed 72 UTF-8 bytes");
        }
        if (userRepository.existsByEmailIgnoreCase(request.getEmail())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "An account already exists for this email address");
        }

        Long reviewerId = reviewerId(adminEmail);
        Long provisionedId;
        UserRole role = request.getRequestedRole();

        if (role == UserRole.TEACHER || role == UserRole.GUARDIAN) {
            IdentityAccountResponse account = identityAccountService.createAccount(
                    new CreateIdentityAccountRequest(
                            role,
                            request.getFullName(),
                            request.getEmail(),
                            request.getPhone(),
                            password,
                            request.getEmployeeCode(),
                            request.getHireDate(),
                            request.getAlternatePhone()
                    ));
            provisionedId = account.profileId();

            if (role == UserRole.GUARDIAN) {
                Student student = studentRepository
                        .findByAdmissionNumberIgnoreCase(request.getStudentAdmissionNumber())
                        .orElseThrow(() -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "The supplied student admission number was not found. Verify it before approving."));
                studentGuardianService.linkGuardian(new StudentGuardianRequest(
                        student.getId(), account.profileId(), request.getRelationship(), false));
            }
        } else {
            var student = studentService.createStudent(new CreateStudentRequest(
                    request.getAdmissionNumber(),
                    request.getFirstName(),
                    request.getLastName(),
                    request.getDateOfBirth(),
                    LocalDate.now(SCHOOL_ZONE),
                    request.getEmail(),
                    request.getPhone(),
                    password
            ));
            provisionedId = student.id();
        }

        String note = normalize(review.reviewNote());
        request.markApproved(reviewerId, provisionedId, note);
        AccountAccessRequest saved = requestRepository.save(request);
        auditService.record(
                "ACCOUNT_ACCESS_REQUEST_APPROVED",
                "ACCOUNT_ACCESS_REQUEST",
                saved.getId().toString(),
                Map.of("status", AccountRequestStatus.PENDING.name()),
                Map.of("status", AccountRequestStatus.APPROVED.name(),
                        "role", role.name(),
                        "provisionedEntityId", provisionedId));
        return toResponse(saved);
    }

    @Transactional
    public AccountAccessRequestResponse reject(
            Long requestId, RejectAccountAccessRequest review, String adminEmail) {
        AccountAccessRequest request = requestRepository.findByIdForUpdate(requestId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Account access request not found"));
        ensurePending(request);

        Long reviewerId = reviewerId(adminEmail);
        String note = review.reviewNote().trim();
        request.markRejected(reviewerId, note);
        AccountAccessRequest saved = requestRepository.save(request);
        auditService.record(
                "ACCOUNT_ACCESS_REQUEST_REJECTED",
                "ACCOUNT_ACCESS_REQUEST",
                saved.getId().toString(),
                Map.of("status", AccountRequestStatus.PENDING.name()),
                Map.of("status", AccountRequestStatus.REJECTED.name()));
        return toResponse(saved);
    }

    private void validateRoleFields(CreateAccountAccessRequest input, UserRole role) {
        if (role == UserRole.TEACHER) {
            require(input.employeeCode(), "Employee code is required for teacher requests");
        } else if (role == UserRole.GUARDIAN) {
            require(input.studentAdmissionNumber(), "Student admission number is required for guardian requests");
            require(input.relationship(), "Relationship to the student is required for guardian requests");
        } else if (role == UserRole.STUDENT) {
            require(input.admissionNumber(), "Admission number is required for student requests");
            require(input.firstName(), "Student first name is required");
            require(input.lastName(), "Student last name is required");
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Only TEACHER, GUARDIAN or STUDENT requests are accepted");
        }
    }

    private void require(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }
    }

    private Long reviewerId(String email) {
        return userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Administrator account was not found"))
                .getId();
    }

    private void ensurePending(AccountAccessRequest request) {
        if (request.getStatus() != AccountRequestStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Only pending account requests can be reviewed");
        }
    }

    private AccountAccessRequestResponse toResponse(AccountAccessRequest r) {
        return new AccountAccessRequestResponse(
                r.getId(), r.getRequestedRole(), r.getFullName(), r.getEmail(), r.getPhone(),
                r.getEmployeeCode(), r.getHireDate(), r.getAlternatePhone(),
                r.getAdmissionNumber(), r.getFirstName(), r.getLastName(), r.getDateOfBirth(),
                r.getStudentAdmissionNumber(), r.getRelationship(), r.getStatus(),
                r.getReviewNote(), r.getReviewedByUserId(), r.getReviewedAt(),
                r.getProvisionedEntityId(), r.getCreatedAt());
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
