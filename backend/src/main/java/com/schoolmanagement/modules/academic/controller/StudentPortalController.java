package com.schoolmanagement.modules.academic.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import com.schoolmanagement.modules.academic.dto.StudentPortalProfileResponse;
import com.schoolmanagement.modules.academic.entity.Student;
import com.schoolmanagement.modules.academic.entity.StudentEnrollment;
import com.schoolmanagement.modules.academic.repository.StudentEnrollmentRepository;
import com.schoolmanagement.modules.academic.repository.StudentRepository;
import com.schoolmanagement.modules.attendance.dto.AttendanceStudentSummaryResponse;
import com.schoolmanagement.modules.attendance.dto.StudentAttendanceHistoryResponse;
import com.schoolmanagement.modules.attendance.entity.AttendanceRecord;
import com.schoolmanagement.modules.attendance.repository.AttendanceRecordRepository;
import com.schoolmanagement.modules.attendance.service.AttendanceReportService;
import com.schoolmanagement.modules.fees.dto.FeeInvoiceResponse;
import com.schoolmanagement.modules.fees.service.FeeInvoiceService;
import com.schoolmanagement.modules.fees.service.FeePaymentService;
import com.schoolmanagement.modules.fees.dto.FeePaymentResponse;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/student")
@PreAuthorize("hasRole('STUDENT')")
public class StudentPortalController {

    private final StudentRepository studentRepository;
    private final StudentEnrollmentRepository enrollmentRepository;
    private final AttendanceReportService attendanceReportService;
    private final AttendanceRecordRepository attendanceRecordRepository;
    private final FeeInvoiceService feeInvoiceService;
    private final FeePaymentService feePaymentService;

    public StudentPortalController(
            StudentRepository studentRepository,
            StudentEnrollmentRepository enrollmentRepository,
            AttendanceReportService attendanceReportService,
            AttendanceRecordRepository attendanceRecordRepository,
            FeeInvoiceService feeInvoiceService,
            FeePaymentService feePaymentService) {
        this.studentRepository = studentRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.attendanceReportService = attendanceReportService;
        this.attendanceRecordRepository = attendanceRecordRepository;
        this.feeInvoiceService = feeInvoiceService;
        this.feePaymentService = feePaymentService;
    }

    @GetMapping("/profile")
    @Transactional(readOnly = true)
    public StudentPortalProfileResponse getProfile(Authentication authentication) {
        Student student = findStudent(authentication.getName());
        StudentEnrollment enrollment = enrollmentRepository
                .findFirstByStudent_IdAndEndDateIsNullOrderByStartDateDesc(student.getId())
                .orElse(null);

        return new StudentPortalProfileResponse(
                student.getId(),
                student.getAdmissionNumber(),
                student.getFirstName(),
                student.getLastName(),
                student.getDateOfBirth(),
                student.getAdmissionDate(),
                student.getStatus(),
                student.getUser().getEmail(),
                student.getUser().getPhone(),
                enrollment == null ? null : enrollment.getSection().getId(),
                enrollment == null ? null : enrollment.getSection().getSectionName(),
                enrollment == null ? null : enrollment.getSection().getSchoolClass().getName(),
                enrollment == null ? null : enrollment.getSection().getAcademicYear(),
                enrollment == null ? null : enrollment.getStartDate()
        );
    }

    @GetMapping("/attendance/summary")
    @Transactional(readOnly = true)
    public AttendanceStudentSummaryResponse getAttendanceSummary(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            Authentication authentication) {
        Student student = findStudent(authentication.getName());
        return attendanceReportService.getStudentSummary(
                student.getId(), null, fromDate, toDate);
    }

    @GetMapping("/attendance/history")
    @Transactional(readOnly = true)
    public List<StudentAttendanceHistoryResponse> getAttendanceHistory(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            Authentication authentication) {
        Student student = findStudent(authentication.getName());
        validateRange(fromDate, toDate);

        return attendanceRecordRepository.findStudentHistory(
                        student.getId(), fromDate, toDate)
                .stream()
                .map(this::toAttendanceHistory)
                .toList();
    }

    @GetMapping("/fees/invoices")
    public Page<FeeInvoiceResponse> getInvoices(
            @RequestParam(required = false) String academicYear,
            @PageableDefault(size = 10, sort = "issueDate", direction = Sort.Direction.DESC)
            Pageable pageable,
            Authentication authentication) {
        return feeInvoiceService.getStudentInvoices(
                authentication.getName(), academicYear, pageable);
    }

    @GetMapping("/fees/invoices/{invoiceId}")
    public FeeInvoiceResponse getInvoice(
            @PathVariable Long invoiceId,
            Authentication authentication) {
        return feeInvoiceService.getStudentInvoice(
                invoiceId, authentication.getName());
    }

    @GetMapping("/fees/invoices/{invoiceId}/payments")
    public List<FeePaymentResponse> getInvoicePayments(
            @PathVariable Long invoiceId,
            Authentication authentication) {
        return feePaymentService.getStudentPaymentHistory(
                invoiceId, authentication.getName());
    }

    private Student findStudent(String email) {
        return studentRepository.findByUser_EmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "No student profile is linked to this account"));
    }

    private void validateRange(LocalDate fromDate, LocalDate toDate) {
        if (fromDate == null || toDate == null || toDate.isBefore(fromDate)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Provide a valid date range; toDate cannot be before fromDate");
        }
    }

    private StudentAttendanceHistoryResponse toAttendanceHistory(AttendanceRecord record) {
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
