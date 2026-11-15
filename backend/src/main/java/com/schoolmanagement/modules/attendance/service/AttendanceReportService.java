package com.schoolmanagement.modules.attendance.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.schoolmanagement.modules.academic.entity.Section;
import com.schoolmanagement.modules.academic.entity.Student;
import com.schoolmanagement.modules.academic.repository.SectionRepository;
import com.schoolmanagement.modules.academic.repository.StudentRepository;

import com.schoolmanagement.modules.attendance.dto.AttendanceSectionSummaryResponse;
import com.schoolmanagement.modules.attendance.dto.AttendanceStudentSummaryResponse;

import com.schoolmanagement.modules.attendance.repository.AttendanceRecordRepository;
import com.schoolmanagement.modules.attendance.repository.AttendanceSessionRepository;
import com.schoolmanagement.modules.attendance.repository.AttendanceStudentAggregate;

@Service
public class AttendanceReportService {

    private final AttendanceRecordRepository recordRepository;
    private final AttendanceSessionRepository sessionRepository;
    private final StudentRepository studentRepository;
    private final SectionRepository sectionRepository;

    public AttendanceReportService(
            AttendanceRecordRepository recordRepository,
            AttendanceSessionRepository sessionRepository,
            StudentRepository studentRepository,
            SectionRepository sectionRepository
    ) {
        this.recordRepository = recordRepository;
        this.sessionRepository = sessionRepository;
        this.studentRepository = studentRepository;
        this.sectionRepository = sectionRepository;
    }

    @Transactional(readOnly = true)
    public AttendanceStudentSummaryResponse getStudentSummary(
            Long studentId,
            Long sectionId,
            LocalDate fromDate,
            LocalDate toDate
    ) {

        validateDates(fromDate, toDate);

        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Student not found"
                ));

        AttendanceStudentAggregate aggregate;

        if (sectionId == null) {
            aggregate = recordRepository.summarizeStudent(
                    studentId,
                    fromDate,
                    toDate
            ).orElse(null);
        } else {
            aggregate = recordRepository.summarizeStudentInSection(
                    studentId,
                    sectionId,
                    fromDate,
                    toDate
            ).orElse(null);
        }

        if (aggregate == null) {
            return new AttendanceStudentSummaryResponse(
                    student.getId(),
                    student.getAdmissionNumber(),
                    student.getFirstName() + " " + student.getLastName(),
                    fromDate,
                    toDate,
                    0L, 0L, 0L, 0L, 0L,
                    BigDecimal.ZERO.setScale(2)
            );
        }

        return toStudentResponse(aggregate, fromDate, toDate);
    }

    @Transactional(readOnly = true)
    public AttendanceSectionSummaryResponse getSectionSummary(
            Long sectionId,
            LocalDate fromDate,
            LocalDate toDate
    ) {

        validateDates(fromDate, toDate);

        Section section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Section not found"
                ));

        List<AttendanceStudentAggregate> aggregates =
                recordRepository.summarizeSection(
                        sectionId,
                        fromDate,
                        toDate
                );

        List<AttendanceStudentSummaryResponse> students =
                new ArrayList<>();

        long totalRecords = 0;
        long present = 0;
        long absent = 0;
        long late = 0;
        long excused = 0;

        for (AttendanceStudentAggregate aggregate : aggregates) {

            students.add(toStudentResponse(
                    aggregate,
                    fromDate,
                    toDate
            ));

            totalRecords += aggregate.getTotalRecords();
            present += aggregate.getPresentCount();
            absent += aggregate.getAbsentCount();
            late += aggregate.getLateCount();
            excused += aggregate.getExcusedCount();
        }

        long totalSessions = sessionRepository.countFinalSessions(
                sectionId,
                fromDate,
                toDate
        );

        return new AttendanceSectionSummaryResponse(
                section.getId(),
                section.getSectionName(),
                section.getSchoolClass().getName(),
                section.getAcademicYear(),

                fromDate,
                toDate,

                totalSessions,
                (long) students.size(),
                totalRecords,

                present,
                absent,
                late,
                excused,

                calculatePercentage(present, late, totalRecords, excused),

                students
        );
    }

    private AttendanceStudentSummaryResponse toStudentResponse(
            AttendanceStudentAggregate aggregate,
            LocalDate fromDate,
            LocalDate toDate
    ) {

        return new AttendanceStudentSummaryResponse(
                aggregate.getStudentId(),
                aggregate.getAdmissionNumber(),
                aggregate.getFirstName() + " " + aggregate.getLastName(),

                fromDate,
                toDate,

                aggregate.getTotalRecords(),
                aggregate.getPresentCount(),
                aggregate.getAbsentCount(),
                aggregate.getLateCount(),
                aggregate.getExcusedCount(),

                calculatePercentage(
                        aggregate.getPresentCount(),
                        aggregate.getLateCount(),
                        aggregate.getTotalRecords(),
                        aggregate.getExcusedCount()
                )
        );
    }

    private BigDecimal calculatePercentage(
            long present,
            long late,
            long total,
            long excused
    ) {

        long denominator = total - excused;

        if (denominator <= 0) {
            return BigDecimal.ZERO.setScale(2);
        }

        return BigDecimal.valueOf(present + late)
                .multiply(BigDecimal.valueOf(100))
                .divide(
                        BigDecimal.valueOf(denominator),
                        2,
                        RoundingMode.HALF_UP
                );
    }

    private void validateDates(
            LocalDate fromDate,
            LocalDate toDate
    ) {

        if (fromDate == null || toDate == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Both fromDate and toDate are required"
            );
        }

        if (toDate.isBefore(fromDate)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "toDate cannot be before fromDate"
            );
        }
    }
}