package com.schoolmanagement.modules.academic.service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.criteria.Predicate;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.schoolmanagement.modules.academic.dto.StudentEnrollmentRequest;
import com.schoolmanagement.modules.academic.dto.StudentEnrollmentResponse;
import com.schoolmanagement.modules.academic.entity.Section;
import com.schoolmanagement.modules.academic.entity.Student;
import com.schoolmanagement.modules.academic.entity.StudentEnrollment;
import com.schoolmanagement.modules.academic.repository.SectionRepository;
import com.schoolmanagement.modules.academic.repository.StudentEnrollmentRepository;
import com.schoolmanagement.modules.academic.repository.StudentRepository;
import com.schoolmanagement.modules.academic.entity.StudentStatus;

@Service
public class StudentEnrollmentService {

    private static final ZoneId SCHOOL_ZONE = ZoneId.of("Asia/Kolkata");

    private final StudentEnrollmentRepository enrollmentRepository;
    private final StudentRepository studentRepository;
    private final SectionRepository sectionRepository;

    public StudentEnrollmentService(
            StudentEnrollmentRepository enrollmentRepository,
            StudentRepository studentRepository,
            SectionRepository sectionRepository
    ) {
        this.enrollmentRepository = enrollmentRepository;
        this.studentRepository = studentRepository;
        this.sectionRepository = sectionRepository;
    }

    @Transactional
    public StudentEnrollmentResponse createEnrollment(
            StudentEnrollmentRequest request
    ) {

        Student student = studentRepository.findById(request.studentId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Student not found"
                ));

        Section section = sectionRepository.findById(request.sectionId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Section not found"
                ));

        if (student.getStatus() != StudentStatus.ACTIVE) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Only active students can be enrolled"
            );
        }

        if (enrollmentRepository
                .existsByStudent_IdAndEndDateIsNull(student.getId())) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Student already has an active enrollment. End the previous enrollment before transferring."
            );
        }

        LocalDate startDate = request.startDate() != null
                ? request.startDate()
                : LocalDate.now(SCHOOL_ZONE);

        StudentEnrollment enrollment = new StudentEnrollment(
                student,
                section,
                startDate
        );

        try {
            StudentEnrollment saved =
                    enrollmentRepository.saveAndFlush(enrollment);

            return toResponse(saved);

        } catch (DataIntegrityViolationException exception) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "An enrollment with these details already exists",
                    exception
            );
        }
    }

    @Transactional(readOnly = true)
    public Page<StudentEnrollmentResponse> getAllEnrollments(
            Long studentId,
            Long sectionId,
            Boolean active,
            LocalDate attendanceDate,
            Pageable pageable
    ) {

        Specification<StudentEnrollment> specification =
                (root, query, cb) -> {

            List<Predicate> predicates = new ArrayList<>();

            if (studentId != null) {
                predicates.add(
                        cb.equal(root.get("student").get("id"), studentId)
                );
            }

            if (sectionId != null) {
                predicates.add(
                        cb.equal(root.get("section").get("id"), sectionId)
                );
            }

            if (active != null) {
                if (active) {
                    predicates.add(cb.isNull(root.get("endDate")));
                } else {
                    predicates.add(cb.isNotNull(root.get("endDate")));
                }
            }

            // Historical registers must use the student's enrollment on the
            // selected record date, not today's active enrollment.
            if (attendanceDate != null) {
                predicates.add(cb.lessThanOrEqualTo(
                        root.<LocalDate>get("startDate"), attendanceDate));
                predicates.add(cb.or(
                        cb.isNull(root.get("endDate")),
                        cb.greaterThanOrEqualTo(
                                root.<LocalDate>get("endDate"), attendanceDate)));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return enrollmentRepository.findAll(specification, pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public StudentEnrollmentResponse getEnrollmentById(Long id) {

        StudentEnrollment enrollment = findEnrollment(id);

        return toResponse(enrollment);
    }

    @Transactional
    public StudentEnrollmentResponse endEnrollment(Long id) {

        StudentEnrollment enrollment = findEnrollment(id);

        if (!enrollment.isActive()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "This enrollment has already ended"
            );
        }

        LocalDate endDate = LocalDate.now(SCHOOL_ZONE);

        if (endDate.isBefore(enrollment.getStartDate())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Enrollment cannot end before its start date"
            );
        }

        enrollment.setEndDate(endDate);

        return toResponse(enrollmentRepository.save(enrollment));
    }

    private StudentEnrollment findEnrollment(Long id) {

        return enrollmentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Enrollment not found"
                ));
    }

    private StudentEnrollmentResponse toResponse(
            StudentEnrollment enrollment
    ) {

        Student student = enrollment.getStudent();
        Section section = enrollment.getSection();

        return new StudentEnrollmentResponse(
                enrollment.getId(),

                student.getId(),
                student.getAdmissionNumber(),
                student.getFirstName() + " " + student.getLastName(),

                section.getId(),
                section.getSectionName(),
                section.getSchoolClass().getName(),
                section.getAcademicYear(),

                enrollment.getStartDate(),
                enrollment.getEndDate(),
                enrollment.isActive()
        );
    }
}