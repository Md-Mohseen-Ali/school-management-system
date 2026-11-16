package com.schoolmanagement.modules.examination.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Map;

import com.schoolmanagement.common.audit.AuditService;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.schoolmanagement.modules.academic.entity.Student;
import com.schoolmanagement.modules.academic.entity.TeacherAssignment;
import com.schoolmanagement.modules.academic.repository.StudentEnrollmentRepository;
import com.schoolmanagement.modules.academic.repository.StudentRepository;
import com.schoolmanagement.modules.academic.repository.TeacherAssignmentRepository;

import com.schoolmanagement.modules.examination.dto.ExaminationMarkEntryRequest;
import com.schoolmanagement.modules.examination.dto.ExaminationMarkResponse;
import com.schoolmanagement.modules.examination.dto.ExaminationMarksRequest;

import com.schoolmanagement.modules.examination.entity.ExamAttendanceStatus;
import com.schoolmanagement.modules.examination.entity.Examination;
import com.schoolmanagement.modules.examination.entity.ExaminationMark;
import com.schoolmanagement.modules.examination.entity.ExaminationStatus;
import com.schoolmanagement.modules.examination.entity.ExaminationSubject;

import com.schoolmanagement.modules.examination.repository.ExaminationMarkRepository;
import com.schoolmanagement.modules.examination.repository.ExaminationSubjectRepository;

import com.schoolmanagement.modules.identity.entity.TeacherProfile;
import com.schoolmanagement.modules.identity.repository.TeacherProfileRepository;

@Service
public class ExaminationMarksService {

    private final ExaminationMarkRepository markRepository;
    private final ExaminationSubjectRepository examinationSubjectRepository;
    private final StudentRepository studentRepository;
    private final StudentEnrollmentRepository enrollmentRepository;
    private final TeacherProfileRepository teacherRepository;
    private final TeacherAssignmentRepository assignmentRepository;
    private final AuditService auditService;

    public ExaminationMarksService(
            ExaminationMarkRepository markRepository,
            ExaminationSubjectRepository examinationSubjectRepository,
            StudentRepository studentRepository,
            StudentEnrollmentRepository enrollmentRepository,
            TeacherProfileRepository teacherRepository,
            TeacherAssignmentRepository assignmentRepository,
            AuditService auditService
    ) {
        this.markRepository = markRepository;
        this.examinationSubjectRepository = examinationSubjectRepository;
        this.studentRepository = studentRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.teacherRepository = teacherRepository;
        this.assignmentRepository = assignmentRepository;
        this.auditService = auditService;
    }

    // ENTER OR UPDATE MARKS

    @Transactional
    public List<ExaminationMarkResponse> saveMarks(
            Long examinationSubjectId,
            ExaminationMarksRequest request,
            String authenticatedEmail,
            boolean admin
    ) {

        ExaminationSubject examinationSubject =
                getExaminationSubject(examinationSubjectId);

        Examination examination = examinationSubject.getExamination();

        ensureScheduled(examination);

        verifyAccess(
                examinationSubject,
                authenticatedEmail,
                admin
        );

        Set<Long> processedStudentIds = new HashSet<>();

        List<ExaminationMark> marksToSave = new ArrayList<>();

        for (ExaminationMarkEntryRequest entry : request.records()) {

            if (!processedStudentIds.add(entry.studentId())) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Duplicate student IDs found in the request"
                );
            }

            Student student = studentRepository.findById(entry.studentId())
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "Student not found: " + entry.studentId()
                    ));

            validateEnrollment(
                    student,
                    examinationSubject
            );

            validateMarks(
                    entry,
                    examinationSubject
            );

            ExaminationMark mark = markRepository
                    .findByExaminationSubject_IdAndStudent_Id(
                            examinationSubjectId,
                            student.getId()
                    )
                    .orElseGet(() -> new ExaminationMark(
                            examinationSubject,
                            student,
                            entry.marksObtained(),
                            entry.attendanceStatus(),
                            normalizeRemarks(entry.remarks())
                    ));

            mark.update(
                    entry.marksObtained(),
                    entry.attendanceStatus(),
                    normalizeRemarks(entry.remarks())
            );

            marksToSave.add(mark);
        }

        try {
            markRepository.saveAllAndFlush(marksToSave);
        } catch (DataIntegrityViolationException exception) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Student marks conflict with existing examination records",
                    exception
            );
        }

        auditService.record("EXAMINATION_MARKS_SAVED", "EXAMINATION_SUBJECT",
                examinationSubject.getId().toString(), null,
                Map.of("examinationId", examination.getId(),
                        "subjectId", examinationSubject.getSubject().getId(),
                        "recordCount", marksToSave.size()));
        return getMarksForSubject(
                examinationSubject,
                authenticatedEmail,
                admin
        );
    }

    // VIEW MARKS FOR AN EXAMINATION SUBJECT

    @Transactional(readOnly = true)
    public List<ExaminationMarkResponse> getMarksForSubject(
            Long examinationSubjectId,
            String authenticatedEmail,
            boolean admin
    ) {

        ExaminationSubject examinationSubject =
                getExaminationSubject(examinationSubjectId);

        verifyAccess(
                examinationSubject,
                authenticatedEmail,
                admin
        );

        return getMarksForSubject(
                examinationSubject,
                authenticatedEmail,
                admin
        );
    }

    private List<ExaminationMarkResponse> getMarksForSubject(
            ExaminationSubject examinationSubject,
            String authenticatedEmail,
            boolean admin
    ) {

        return markRepository
                .findByExaminationSubject_IdOrderByStudent_AdmissionNumberAsc(
                        examinationSubject.getId()
                )
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private ExaminationSubject getExaminationSubject(Long id) {

        return examinationSubjectRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Examination subject not found"
                ));
    }

    private void ensureScheduled(Examination examination) {

        if (examination.getStatus() != ExaminationStatus.SCHEDULED) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Marks can only be entered for scheduled examinations"
            );
        }
    }

    private void verifyAccess(
            ExaminationSubject examinationSubject,
            String authenticatedEmail,
            boolean admin
    ) {

        if (admin) {
            return;
        }

        TeacherProfile teacher = teacherRepository
                .findActiveByUserEmail(authenticatedEmail)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "No active teacher profile found"
                ));

        Long sectionId = examinationSubject
                .getExamination()
                .getSection()
                .getId();

        Long subjectId = examinationSubject
                .getSubject()
                .getId();

        boolean assigned = assignmentRepository
                .existsByTeacher_IdAndSection_IdAndSubject_IdAndActiveTrue(
                        teacher.getId(),
                        sectionId,
                        subjectId
                );

        if (!assigned) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You are not assigned to this examination subject"
            );
        }
    }

    private void validateEnrollment(
            Student student,
            ExaminationSubject examinationSubject
    ) {

        Long sectionId = examinationSubject
                .getExamination()
                .getSection()
                .getId();

        boolean enrolled = enrollmentRepository.existsEnrollmentOnDate(
                student.getId(),
                sectionId,
                examinationSubject.getExaminationDate()
        );

        if (!enrolled) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Student " + student.getId()
                            + " was not enrolled in this section on the examination date"
            );
        }
    }

    private void validateMarks(
            ExaminationMarkEntryRequest entry,
            ExaminationSubject examinationSubject
    ) {

        BigDecimal obtained = entry.marksObtained();
        BigDecimal maximum = examinationSubject.getMaxMarks();

        if (obtained.compareTo(maximum) > 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Marks obtained cannot exceed maximum marks: " + maximum
            );
        }

        if (entry.attendanceStatus() == ExamAttendanceStatus.ABSENT
                && obtained.compareTo(BigDecimal.ZERO) != 0) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Absent students must receive zero marks"
            );
        }
    }

    private String normalizeRemarks(String remarks) {

        if (remarks == null || remarks.isBlank()) {
            return null;
        }

        return remarks.trim();
    }

    private ExaminationMarkResponse toResponse(ExaminationMark mark) {

        Student student = mark.getStudent();
        ExaminationSubject examinationSubject =
                mark.getExaminationSubject();

        boolean passed =
                mark.getAttendanceStatus() == ExamAttendanceStatus.PRESENT
                && mark.getMarksObtained().compareTo(
                        examinationSubject.getPassMarks()
                ) >= 0;

        return new ExaminationMarkResponse(
                mark.getId(),

                examinationSubject.getId(),

                student.getId(),
                student.getAdmissionNumber(),
                student.getFirstName() + " " + student.getLastName(),

                mark.getMarksObtained(),
                examinationSubject.getMaxMarks(),
                examinationSubject.getPassMarks(),

                mark.getAttendanceStatus(),
                passed,

                mark.getRemarks(),

                mark.getEnteredAt(),
                mark.getUpdatedAt()
        );
    }
}