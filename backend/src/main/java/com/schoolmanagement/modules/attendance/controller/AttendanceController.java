package com.schoolmanagement.modules.attendance.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;

import org.springframework.web.bind.annotation.*;

import com.schoolmanagement.modules.attendance.dto.AttendanceRecordsRequest;
import com.schoolmanagement.modules.attendance.dto.AttendanceRecordResponse;
import com.schoolmanagement.modules.attendance.dto.AttendanceSessionRequest;
import com.schoolmanagement.modules.attendance.dto.AttendanceSessionResponse;

import com.schoolmanagement.modules.attendance.entity.AttendanceSessionStatus;
import com.schoolmanagement.modules.attendance.service.AttendanceService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/attendance/sessions")
@PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
public class AttendanceController {

    private final AttendanceService attendanceService;

    public AttendanceController(AttendanceService attendanceService) {
        this.attendanceService = attendanceService;
    }

    private boolean isAdmin(Authentication authentication) {

        return authentication.getAuthorities().stream()
                .anyMatch(authority ->
                        authority.getAuthority().equals("ROLE_ADMIN"));
    }

    // Create session: TEACHER only
    @PostMapping
    @PreAuthorize("hasRole('TEACHER')")
    public AttendanceSessionResponse createSession(
            @Valid @RequestBody AttendanceSessionRequest request,
            Authentication authentication
    ) {
        return attendanceService.createSession(
                request,
                authentication.getName()
        );
    }

    // List sessions: ADMIN sees all; TEACHER sees their own
    @GetMapping
    public Page<AttendanceSessionResponse> getAllSessions(
            @RequestParam(required = false) Long sectionId,
            @RequestParam(required = false) LocalDate attendanceDate,
            @RequestParam(required = false) AttendanceSessionStatus status,

            @PageableDefault(
                size = 20,
                sort = "attendanceDate",
                direction = Sort.Direction.DESC
            )
            Pageable pageable,

            Authentication authentication
    ) {
        return attendanceService.getAllSessions(
                authentication.getName(),
                isAdmin(authentication),
                sectionId,
                attendanceDate,
                status,
                pageable
        );
    }

    // Get one session
    @GetMapping("/{sessionId}")
    public AttendanceSessionResponse getSessionById(
            @PathVariable Long sessionId,
            Authentication authentication
    ) {
        return attendanceService.getSessionById(
                sessionId,
                authentication.getName(),
                isAdmin(authentication)
        );
    }

    // Add or update multiple student records in a DRAFT session
    @PutMapping("/{sessionId}/records")
    @PreAuthorize("hasRole('TEACHER')")
    public List<AttendanceRecordResponse> saveRecords(
            @PathVariable Long sessionId,
            @Valid @RequestBody AttendanceRecordsRequest request,
            Authentication authentication
    ) {
        return attendanceService.saveRecords(
                sessionId,
                request,
                authentication.getName()
        );
    }

    // View attendance records
    @GetMapping("/{sessionId}/records")
    public List<AttendanceRecordResponse> getSessionRecords(
            @PathVariable Long sessionId,
            Authentication authentication
    ) {
        return attendanceService.getSessionRecords(
                sessionId,
                authentication.getName(),
                isAdmin(authentication)
        );
    }

    // Submit attendance: TEACHER only
    @PostMapping("/{sessionId}/submit")
    @PreAuthorize("hasRole('TEACHER')")
    public AttendanceSessionResponse submitSession(
            @PathVariable Long sessionId,
            Authentication authentication
    ) {
        return attendanceService.submitSession(
                sessionId,
                authentication.getName()
        );
    }

    // Lock submitted attendance: ADMIN only
    @PostMapping("/{sessionId}/lock")
    @PreAuthorize("hasRole('ADMIN')")
    public AttendanceSessionResponse lockSession(
            @PathVariable Long sessionId
    ) {
        return attendanceService.lockSession(sessionId);
    }
}