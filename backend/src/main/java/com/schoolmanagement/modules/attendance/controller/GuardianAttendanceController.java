package com.schoolmanagement.modules.attendance.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import com.schoolmanagement.modules.academic.repository.StudentGuardianRepository;
import com.schoolmanagement.modules.attendance.dto.AttendanceStudentSummaryResponse;
import com.schoolmanagement.modules.attendance.dto.StudentAttendanceHistoryResponse;
import com.schoolmanagement.modules.attendance.entity.AttendanceRecord;
import com.schoolmanagement.modules.attendance.repository.AttendanceRecordRepository;
import com.schoolmanagement.modules.attendance.service.AttendanceReportService;
import com.schoolmanagement.modules.identity.entity.GuardianProfile;
import com.schoolmanagement.modules.identity.repository.GuardianProfileRepository;

@RestController
@RequestMapping("/api/v1/guardian/students/{studentId}/attendance")
@PreAuthorize("hasRole('GUARDIAN')")
public class GuardianAttendanceController {

    private final GuardianProfileRepository guardianRepository;
    private final StudentGuardianRepository studentGuardianRepository;
    private final AttendanceReportService attendanceReportService;
    private final AttendanceRecordRepository attendanceRecordRepository;

    public GuardianAttendanceController(
            GuardianProfileRepository guardianRepository,
            StudentGuardianRepository studentGuardianRepository,
            AttendanceReportService attendanceReportService,
            AttendanceRecordRepository attendanceRecordRepository) {
        this.guardianRepository = guardianRepository;
        this.studentGuardianRepository = studentGuardianRepository;
        this.attendanceReportService = attendanceReportService;
        this.attendanceRecordRepository = attendanceRecordRepository;
    }

    @GetMapping("/summary")
    @Transactional(readOnly = true)
    public AttendanceStudentSummaryResponse getSummary(
            @PathVariable Long studentId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            Authentication authentication) {
        verifyLinkedStudent(authentication.getName(), studentId);
        return attendanceReportService.getStudentSummary(
                studentId, null, fromDate, toDate);
    }

    @GetMapping("/history")
    @Transactional(readOnly = true)
    public List<StudentAttendanceHistoryResponse> getHistory(
            @PathVariable Long studentId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            Authentication authentication) {
        verifyLinkedStudent(authentication.getName(), studentId);
        if (fromDate == null || toDate == null || toDate.isBefore(fromDate)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Provide a valid date range; toDate cannot be before fromDate");
        }

        return attendanceRecordRepository.findStudentHistory(studentId, fromDate, toDate)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private void verifyLinkedStudent(String email, Long studentId) {
        GuardianProfile guardian = guardianRepository.findByUserEmail(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Guardian profile not found"));

        if (!studentGuardianRepository.existsByStudent_IdAndGuardian_Id(
                studentId, guardian.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Student not found for this guardian");
        }
    }

    private StudentAttendanceHistoryResponse toResponse(AttendanceRecord record) {
        var session = record.getAttendanceSession();
        var section = session.getSection();
        return new StudentAttendanceHistoryResponse(
                record.getId(),
                session.getId(),
                session.getAttendanceDate(),
                session.getSessionCode(),
                section.getSchoolClass().getName(),
                section.getSectionName(),
                session.getStatus(),
                record.getAttendanceStatus(),
                record.getRemarks(),
                record.getMarkedAt()
        );
    }
}
