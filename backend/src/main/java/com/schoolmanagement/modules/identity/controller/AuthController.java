package com.schoolmanagement.modules.identity.controller;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.LinkedHashMap;

import org.springframework.data.domain.PageRequest;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import com.schoolmanagement.modules.academic.entity.Student;
import com.schoolmanagement.modules.academic.entity.StudentEnrollment;
import com.schoolmanagement.modules.academic.repository.StudentEnrollmentRepository;
import com.schoolmanagement.modules.academic.repository.StudentGuardianRepository;
import com.schoolmanagement.modules.academic.repository.StudentRepository;
import com.schoolmanagement.modules.academic.repository.TeacherAssignmentRepository;
import com.schoolmanagement.modules.identity.entity.AppUser;
import com.schoolmanagement.modules.identity.entity.GuardianProfile;
import com.schoolmanagement.modules.identity.entity.TeacherProfile;
import com.schoolmanagement.modules.identity.repository.AppUserRepository;
import com.schoolmanagement.modules.identity.repository.GuardianProfileRepository;
import com.schoolmanagement.modules.identity.repository.TeacherProfileRepository;
import com.schoolmanagement.modules.identity.dto.AuthResponse;
import com.schoolmanagement.modules.identity.dto.LoginRequest;
import com.schoolmanagement.security.jwt.JwtService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private static final ZoneId SCHOOL_ZONE = ZoneId.of("Asia/Kolkata");

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final AppUserRepository userRepository;
    private final StudentRepository studentRepository;
    private final StudentEnrollmentRepository enrollmentRepository;
    private final TeacherProfileRepository teacherRepository;
    private final TeacherAssignmentRepository assignmentRepository;
    private final GuardianProfileRepository guardianRepository;
    private final StudentGuardianRepository studentGuardianRepository;

    public AuthController(
            AuthenticationManager authenticationManager,
            JwtService jwtService,
            AppUserRepository userRepository,
            StudentRepository studentRepository,
            StudentEnrollmentRepository enrollmentRepository,
            TeacherProfileRepository teacherRepository,
            TeacherAssignmentRepository assignmentRepository,
            GuardianProfileRepository guardianRepository,
            StudentGuardianRepository studentGuardianRepository) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.studentRepository = studentRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.teacherRepository = teacherRepository;
        this.assignmentRepository = assignmentRepository;
        this.guardianRepository = guardianRepository;
        this.studentGuardianRepository = studentGuardianRepository;
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        try {
            var authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.email().trim(),
                            request.password()));

            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            String token = jwtService.generateAccessToken(userDetails);

            return new AuthResponse(
                    token,
                    "Bearer",
                    jwtService.getAccessTokenValiditySeconds());
        } catch (AuthenticationException exception) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Invalid email or password");
        }
    }

    @GetMapping("/me")
    @Transactional(readOnly = true)
    public Map<String, Object> getCurrentUser(Authentication authentication) {
        AppUser user = userRepository.findByEmailIgnoreCase(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Authenticated account was not found"));

        List<String> roles = authentication.getAuthorities().stream()
                .map(authority -> authority.getAuthority())
                .toList();

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id", user.getId());
        response.put("fullName", user.getFullName());
        response.put("email", user.getEmail());
        response.put("phone", user.getPhone());
        response.put("roles", roles);
        response.put("enabled", user.isEnabled());
        response.put("createdAt", user.getCreatedAt());
        response.put("authenticated", authentication.isAuthenticated());
        response.put("profile", buildRoleProfile(user, roles));
        return response;
    }

    private Map<String, Object> buildRoleProfile(AppUser user, List<String> roles) {
        Map<String, Object> profile = new LinkedHashMap<>();

        if (roles.contains("ROLE_STUDENT")) {
            profile.put("type", "STUDENT");
            studentRepository.findByUser_EmailIgnoreCase(user.getEmail()).ifPresent(student -> {
                profile.put("studentId", student.getId());
                profile.put("admissionNumber", student.getAdmissionNumber());
                profile.put("firstName", student.getFirstName());
                profile.put("lastName", student.getLastName());
                profile.put("dateOfBirth", student.getDateOfBirth());
                profile.put("admissionDate", student.getAdmissionDate());
                profile.put("studentStatus", student.getStatus().name());
                enrollmentRepository.findCurrentEnrollments(student.getId(), LocalDate.now(SCHOOL_ZONE), PageRequest.of(0, 1)).stream().findFirst()
                        .ifPresent(enrollment -> addEnrollment(profile, enrollment));
            });
        } else if (roles.contains("ROLE_TEACHER")) {
            profile.put("type", "TEACHER");
            teacherRepository.findByUserId(user.getId()).ifPresent(teacher -> {
                profile.put("teacherId", teacher.getId());
                profile.put("employeeCode", teacher.getEmployeeCode());
                profile.put("hireDate", teacher.getHireDate());
                profile.put("active", teacher.isActive());
                profile.put("assignedSections", assignmentRepository.findByTeacher_IdAndActiveTrue(teacher.getId())
                        .stream().map(assignment -> {
                            Map<String, Object> item = new LinkedHashMap<>();
                            item.put("sectionId", assignment.getSection().getId());
                            item.put("sectionName", assignment.getSection().getSectionName());
                            item.put("className", assignment.getSection().getSchoolClass().getName());
                            item.put("academicYear", assignment.getSection().getAcademicYear());
                            item.put("subjectName", assignment.getSubject().getName());
                            item.put("subjectCode", assignment.getSubject().getSubjectCode());
                            item.put("assignedOn", assignment.getAssignedOn());
                            return item;
                        }).toList());
            });
        } else if (roles.contains("ROLE_GUARDIAN")) {
            profile.put("type", "GUARDIAN");
            guardianRepository.findByUserEmail(user.getEmail()).ifPresent(guardian -> {
                profile.put("guardianId", guardian.getId());
                profile.put("alternatePhone", guardian.getAlternatePhone());
                profile.put("linkedStudents", studentGuardianRepository.findByGuardian_Id(guardian.getId())
                        .stream().map(link -> {
                            Student student = link.getStudent();
                            Map<String, Object> item = new LinkedHashMap<>();
                            item.put("studentId", student.getId());
                            item.put("admissionNumber", student.getAdmissionNumber());
                            item.put("studentName", student.getFirstName() + " " + student.getLastName());
                            item.put("relationship", link.getRelationship());
                            item.put("primary", link.isPrimaryGuardian());
                            item.put("studentStatus", student.getStatus().name());
                            enrollmentRepository.findCurrentEnrollment(student.getId(), LocalDate.now(SCHOOL_ZONE))
                                    .ifPresent(enrollment -> {
                                        item.put("className", enrollment.getSection().getSchoolClass().getName());
                                        item.put("sectionName", enrollment.getSection().getSectionName());
                                        item.put("academicYear", enrollment.getSection().getAcademicYear());
                                    });
                            return item;
                        }).toList());
            });
        } else if (roles.contains("ROLE_ADMIN")) {
            profile.put("type", "ADMIN");
            profile.put("title", "School administrator");
            profile.put("administrativeScope", "School-wide");
            profile.put("managementAreas", List.of(
                    "People and access", "Academic administration", "Attendance",
                    "Examinations and grading", "Fees and payments", "Audit trail"
            ));
        } else {
            profile.put("type", "ACCOUNT");
        }

        return profile;
    }

    private void addEnrollment(Map<String, Object> profile, StudentEnrollment enrollment) {
        profile.put("sectionId", enrollment.getSection().getId());
        profile.put("sectionName", enrollment.getSection().getSectionName());
        profile.put("className", enrollment.getSection().getSchoolClass().getName());
        profile.put("academicYear", enrollment.getSection().getAcademicYear());
        profile.put("enrollmentStartDate", enrollment.getStartDate());
    }
}
