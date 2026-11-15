package com.schoolmanagement.modules.attendance.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import jakarta.persistence.criteria.Predicate;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.schoolmanagement.modules.academic.entity.Section;
import com.schoolmanagement.modules.academic.entity.Student;
import com.schoolmanagement.modules.academic.entity.StudentEnrollment;
import com.schoolmanagement.modules.academic.repository.SectionRepository;
import com.schoolmanagement.modules.academic.repository.StudentEnrollmentRepository;
import com.schoolmanagement.modules.academic.repository.StudentRepository;
import com.schoolmanagement.modules.academic.repository.TeacherAssignmentRepository;

import com.schoolmanagement.modules.attendance.dto.AttendanceEntryRequest;
import com.schoolmanagement.modules.attendance.dto.AttendanceRecordResponse;
import com.schoolmanagement.modules.attendance.dto.AttendanceRecordsRequest;
import com.schoolmanagement.modules.attendance.dto.AttendanceSessionRequest;
import com.schoolmanagement.modules.attendance.dto.AttendanceSessionResponse;

import com.schoolmanagement.modules.attendance.entity.AttendanceRecord;
import com.schoolmanagement.modules.attendance.entity.AttendanceRecordStatus;
import com.schoolmanagement.modules.attendance.entity.AttendanceSession;
import com.schoolmanagement.modules.attendance.entity.AttendanceSessionStatus;

import com.schoolmanagement.modules.attendance.repository.AttendanceRecordRepository;
import com.schoolmanagement.modules.attendance.repository.AttendanceSessionRepository;

import com.schoolmanagement.modules.identity.entity.TeacherProfile;
import com.schoolmanagement.modules.identity.repository.TeacherProfileRepository;

@Service
public class AttendanceService {

    private final AttendanceSessionRepository sessionRepository;
    private final AttendanceRecordRepository recordRepository;

    private final TeacherProfileRepository teacherRepository;
    private final TeacherAssignmentRepository assignmentRepository;

    private final SectionRepository sectionRepository;
    private final StudentRepository studentRepository;
    private final StudentEnrollmentRepository enrollmentRepository;

    public AttendanceService(
            AttendanceSessionRepository sessionRepository,
            AttendanceRecordRepository recordRepository,
            TeacherProfileRepository teacherRepository,
            TeacherAssignmentRepository assignmentRepository,
            SectionRepository sectionRepository,
            StudentRepository studentRepository,
            StudentEnrollmentRepository enrollmentRepository
    ) {
        this.sessionRepository = sessionRepository;
        this.recordRepository = recordRepository;
        this.teacherRepository = teacherRepository;
        this.assignmentRepository = assignmentRepository;
        this.sectionRepository = sectionRepository;
        this.studentRepository = studentRepository;
        this.enrollmentRepository = enrollmentRepository;
    }

    // ------------------------------------------------
    // CREATE ATTENDANCE SESSION
    // ------------------------------------------------

    @Transactional
    public AttendanceSessionResponse createSession(
            AttendanceSessionRequest request,
            String authenticatedEmail
    ) {

        TeacherProfile teacher = getAuthenticatedTeacher(authenticatedEmail);

        Section section = sectionRepository.findById(request.sectionId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Section not found"
                ));

        boolean assigned = assignmentRepository
                .existsByTeacher_IdAndSection_IdAndActiveTrue(
                        teacher.getId(),
                        section.getId()
                );

        if (!assigned) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You are not assigned to this section"
            );
        }

        String sessionCode = normalizeSessionCode(request.sessionCode());

        if (sessionRepository
                .existsBySection_IdAndAttendanceDateAndSessionCode(
                        section.getId(),
                        request.attendanceDate(),
                        sessionCode
                )) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "An attendance session already exists for this section, date and session code"
            );
        }

        AttendanceSession session = new AttendanceSession(
                section,
                teacher,
                request.attendanceDate(),
                sessionCode,
                normalizeNotes(request.notes())
        );

        try {
            return toSessionResponse(
                    sessionRepository.saveAndFlush(session)
            );
        } catch (DataIntegrityViolationException exception) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "An attendance session with these details already exists",
                    exception
            );
        }
    }

    // ------------------------------------------------
    // LIST ATTENDANCE SESSIONS
    // ------------------------------------------------

    @Transactional(readOnly = true)
    public Page<AttendanceSessionResponse> getAllSessions(
            String authenticatedEmail,
            boolean admin,
            Long sectionId,
            LocalDate attendanceDate,
            AttendanceSessionStatus status,
            Pageable pageable
    ) {

        TeacherProfile teacher = admin
                ? null
                : getAuthenticatedTeacher(authenticatedEmail);

        Specification<AttendanceSession> specification =
                (root, query, cb) -> {

            List<Predicate> predicates = new ArrayList<>();

            if (!admin) {
                predicates.add(
                        cb.equal(
                                root.get("markedByTeacher").get("id"),
                                teacher.getId()
                        )
                );
            }

            if (sectionId != null) {
                predicates.add(
                        cb.equal(root.get("section").get("id"), sectionId)
                );
            }

            if (attendanceDate != null) {
                predicates.add(
                        cb.equal(root.get("attendanceDate"), attendanceDate)
                );
            }

            if (status != null) {
                predicates.add(
                        cb.equal(root.get("status"), status)
                );
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return sessionRepository.findAll(specification, pageable)
                .map(this::toSessionResponse);
    }

    // ------------------------------------------------
    // GET SINGLE SESSION
    // ------------------------------------------------

    @Transactional(readOnly = true)
    public AttendanceSessionResponse getSessionById(
            Long sessionId,
            String authenticatedEmail,
            boolean admin
    ) {

        AttendanceSession session = findSession(sessionId);

        verifySessionAccess(session, authenticatedEmail, admin);

        return toSessionResponse(session);
    }

    // ------------------------------------------------
    // ADD OR UPDATE ATTENDANCE RECORDS IN A DRAFT
    // ------------------------------------------------

    @Transactional
    public List<AttendanceRecordResponse> saveRecords(
            Long sessionId,
            AttendanceRecordsRequest request,
            String authenticatedEmail
    ) {

        AttendanceSession session = findSessionForUpdate(sessionId);

        verifySessionAccess(session, authenticatedEmail, false);
        ensureDraft(session);

        Set<Long> submittedStudentIds = new HashSet<>();

        List<AttendanceRecord> recordsToSave = new ArrayList<>();

        for (AttendanceEntryRequest entry : request.records()) {

            if (!submittedStudentIds.add(entry.studentId())) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "The request contains duplicate student IDs"
                );
            }

            Student student = studentRepository.findById(entry.studentId())
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "Student not found: " + entry.studentId()
                    ));

            boolean enrolled = enrollmentRepository.existsEnrollmentOnDate(
                    student.getId(),
                    session.getSection().getId(),
                    session.getAttendanceDate()
            );

            if (!enrolled) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Student " + student.getId()
                                + " was not enrolled in this section on the attendance date"
                );
            }

            AttendanceRecord record = recordRepository
                    .findByAttendanceSession_IdAndStudent_Id(
                            sessionId,
                            student.getId()
                    )
                    .orElseGet(() -> new AttendanceRecord(
                            session,
                            student,
                            entry.attendanceStatus(),
                            normalizeNotes(entry.remarks())
                    ));

            record.update(
                    entry.attendanceStatus(),
                    normalizeNotes(entry.remarks())
            );

            recordsToSave.add(record);
        }

        try {
            recordRepository.saveAllAndFlush(recordsToSave);
        } catch (DataIntegrityViolationException exception) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Attendance records conflict with existing records",
                    exception
            );
        }

        return recordRepository
                .findByAttendanceSession_IdOrderByStudent_AdmissionNumberAsc(
                        sessionId
                )
                .stream()
                .map(this::toRecordResponse)
                .toList();
    }

    // ------------------------------------------------
    // GET RECORDS FOR A SESSION
    // ------------------------------------------------

    @Transactional(readOnly = true)
    public List<AttendanceRecordResponse> getSessionRecords(
            Long sessionId,
            String authenticatedEmail,
            boolean admin
    ) {

        AttendanceSession session = findSession(sessionId);

        verifySessionAccess(session, authenticatedEmail, admin);

        return recordRepository
                .findByAttendanceSession_IdOrderByStudent_AdmissionNumberAsc(
                        sessionId
                )
                .stream()
                .map(this::toRecordResponse)
                .toList();
    }

    // ------------------------------------------------
    // SUBMIT ATTENDANCE
    // ------------------------------------------------

    @Transactional
    public AttendanceSessionResponse submitSession(
            Long sessionId,
            String authenticatedEmail
    ) {

        AttendanceSession session = findSessionForUpdate(sessionId);

        verifySessionAccess(session, authenticatedEmail, false);
        ensureDraft(session);

        List<StudentEnrollment> enrollments =
                enrollmentRepository.findEnrollmentsOnDate(
                        session.getSection().getId(),
                        session.getAttendanceDate()
                );

        if (enrollments.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "There are no enrolled students for this section on the attendance date"
            );
        }

        Set<Long> expectedStudentIds = new HashSet<>();

        for (StudentEnrollment enrollment : enrollments) {
            expectedStudentIds.add(enrollment.getStudent().getId());
        }

        List<AttendanceRecord> records =
                recordRepository
                        .findByAttendanceSession_IdOrderByStudent_AdmissionNumberAsc(
                                sessionId
                        );

        Set<Long> markedStudentIds = new HashSet<>();

        for (AttendanceRecord record : records) {
            markedStudentIds.add(record.getStudent().getId());
        }

        if (!expectedStudentIds.equals(markedStudentIds)) {
            Set<Long> missingStudentIds =
                    new HashSet<>(expectedStudentIds);

            missingStudentIds.removeAll(markedStudentIds);

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Attendance is incomplete. Missing student IDs: "
                            + missingStudentIds
            );
        }

        session.submit();

        return toSessionResponse(sessionRepository.save(session));
    }

    // ------------------------------------------------
    // LOCK SUBMITTED ATTENDANCE (ADMIN)
    // ------------------------------------------------

    @Transactional
    public AttendanceSessionResponse lockSession(Long sessionId) {

        AttendanceSession session = findSessionForUpdate(sessionId);

        if (session.getStatus() != AttendanceSessionStatus.SUBMITTED) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Only submitted attendance sessions can be locked"
            );
        }

        session.lock();

        return toSessionResponse(sessionRepository.save(session));
    }

    // ------------------------------------------------
    // PRIVATE HELPERS
    // ------------------------------------------------

    private TeacherProfile getAuthenticatedTeacher(String email) {

        return teacherRepository.findActiveByUserEmail(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "No active teacher profile is associated with this account"
                ));
    }

    private AttendanceSession findSession(Long id) {

        return sessionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Attendance session not found"
                ));
    }

    private AttendanceSession findSessionForUpdate(Long id) {

        return sessionRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Attendance session not found"
                ));
    }

    private void verifySessionAccess(
            AttendanceSession session,
            String email,
            boolean admin
    ) {

        if (admin) {
            return;
        }

        TeacherProfile teacher = getAuthenticatedTeacher(email);

        if (!session.getMarkedByTeacher().getId().equals(teacher.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You cannot access another teacher's attendance session"
            );
        }
    }

    private void ensureDraft(AttendanceSession session) {

        if (session.getStatus() != AttendanceSessionStatus.DRAFT) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Attendance can only be modified while the session is in DRAFT status"
            );
        }
    }

    private String normalizeSessionCode(String sessionCode) {

        if (sessionCode == null || sessionCode.isBlank()) {
            return "FULL_DAY";
        }

        String normalized = sessionCode.trim().toUpperCase(Locale.ROOT);

        if (normalized.length() > 20) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Session code cannot exceed 20 characters"
            );
        }

        return normalized;
    }

    private String normalizeNotes(String value) {

        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }

    private AttendanceSessionResponse toSessionResponse(
            AttendanceSession session
    ) {

        Section section = session.getSection();
        TeacherProfile teacher = session.getMarkedByTeacher();

        return new AttendanceSessionResponse(
                session.getId(),

                section.getId(),
                section.getSectionName(),
                section.getSchoolClass().getName(),
                section.getAcademicYear(),

                teacher.getId(),
                teacher.getEmployeeCode(),

                session.getAttendanceDate(),
                session.getSessionCode(),

                session.getStatus(),
                session.getNotes(),

                session.getCreatedAt(),
                session.getSubmittedAt()
        );
    }

    private AttendanceRecordResponse toRecordResponse(
            AttendanceRecord record
    ) {

        Student student = record.getStudent();

        return new AttendanceRecordResponse(
                record.getId(),

                student.getId(),
                student.getAdmissionNumber(),
                student.getFirstName() + " " + student.getLastName(),

                record.getAttendanceStatus(),
                record.getRemarks(),

                record.getMarkedAt(),
                record.getUpdatedAt()
        );
    }
}