package com.schoolmanagement.modules.academic.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.schoolmanagement.common.audit.AuditService;

import jakarta.persistence.criteria.Predicate;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.schoolmanagement.modules.academic.dto.TeacherAssignmentRequest;
import com.schoolmanagement.modules.academic.dto.TeacherAssignmentResponse;
import com.schoolmanagement.modules.academic.entity.Section;
import com.schoolmanagement.modules.academic.entity.Subject;
import com.schoolmanagement.modules.academic.entity.TeacherAssignment;
import com.schoolmanagement.modules.academic.repository.SectionRepository;
import com.schoolmanagement.modules.academic.repository.SubjectRepository;
import com.schoolmanagement.modules.academic.repository.TeacherAssignmentRepository;

import com.schoolmanagement.modules.identity.entity.TeacherProfile;
import com.schoolmanagement.modules.identity.repository.TeacherProfileRepository;

@Service
public class TeacherAssignmentService {

    private final TeacherAssignmentRepository assignmentRepository;
    private final TeacherProfileRepository teacherRepository;
    private final SectionRepository sectionRepository;
    private final SubjectRepository subjectRepository;
    private final AuditService auditService;

    public TeacherAssignmentService(
            TeacherAssignmentRepository assignmentRepository,
            TeacherProfileRepository teacherRepository,
            SectionRepository sectionRepository,
            SubjectRepository subjectRepository,
            AuditService auditService
    ) {
        this.assignmentRepository = assignmentRepository;
        this.teacherRepository = teacherRepository;
        this.sectionRepository = sectionRepository;
        this.subjectRepository = subjectRepository;
        this.auditService = auditService;
    }

    @Transactional
    public TeacherAssignmentResponse createAssignment(
            TeacherAssignmentRequest request
    ) {

        TeacherProfile teacher = getTeacher(request.teacherId());
        Section section = getSection(request.sectionId());
        Subject subject = getSubject(request.subjectId());

        validateTeacherAndSubject(teacher, subject);

        if (assignmentRepository
                .existsByTeacher_IdAndSection_IdAndSubject_IdAndActiveTrue(
                        teacher.getId(),
                        section.getId(),
                        subject.getId()
                )) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "This teacher is already assigned to this subject in this section"
            );
        }

        TeacherAssignment assignment = new TeacherAssignment(
                teacher,
                section,
                subject
        );

        try {
            TeacherAssignment saved = assignmentRepository.saveAndFlush(assignment);
            auditService.record("TEACHER_ASSIGNMENT_CREATED", "TEACHER_ASSIGNMENT",
                    saved.getId().toString(), null,
                    Map.of("teacherId", teacher.getId(),
                            "sectionId", section.getId(),
                            "subjectId", subject.getId()));
            return toResponse(saved);
        } catch (DataIntegrityViolationException exception) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "An active assignment with these details already exists",
                    exception
            );
        }
    }

    @Transactional(readOnly = true)
    public Page<TeacherAssignmentResponse> getAllAssignments(
            Long teacherId,
            Long sectionId,
            Long subjectId,
            Boolean active,
            Pageable pageable
    ) {

        Specification<TeacherAssignment> specification =
                (root, query, cb) -> {

            List<Predicate> predicates = new ArrayList<>();

            if (teacherId != null) {
                predicates.add(
                        cb.equal(root.get("teacher").get("id"), teacherId)
                );
            }

            if (sectionId != null) {
                predicates.add(
                        cb.equal(root.get("section").get("id"), sectionId)
                );
            }

            if (subjectId != null) {
                predicates.add(
                        cb.equal(root.get("subject").get("id"), subjectId)
                );
            }

            if (active != null) {
                predicates.add(
                        cb.equal(root.get("active"), active)
                );
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return assignmentRepository.findAll(specification, pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public TeacherAssignmentResponse getAssignmentById(Long id) {

        return toResponse(getAssignment(id));
    }

    @Transactional
    public TeacherAssignmentResponse updateAssignmentStatus(
            Long id,
            boolean active
    ) {

        TeacherAssignment assignment = getAssignment(id);

        if (active) {

            validateTeacherAndSubject(
                    assignment.getTeacher(),
                    assignment.getSubject()
            );

            if (assignmentRepository
                    .existsByTeacher_IdAndSection_IdAndSubject_IdAndActiveTrueAndIdNot(
                            assignment.getTeacher().getId(),
                            assignment.getSection().getId(),
                            assignment.getSubject().getId(),
                            id
                    )) {

                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Another active assignment already exists for this teacher, section and subject"
                );
            }
        }

        boolean oldActive = assignment.isActive();
        assignment.setActive(active);

        try {
            TeacherAssignment saved = assignmentRepository.saveAndFlush(assignment);
            auditService.record("TEACHER_ASSIGNMENT_STATUS_CHANGED", "TEACHER_ASSIGNMENT",
                    saved.getId().toString(), Map.of("active", oldActive),
                    Map.of("active", saved.isActive()));
            return toResponse(saved);
        } catch (DataIntegrityViolationException exception) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "An active assignment with these details already exists",
                    exception
            );
        }
    }

    private TeacherProfile getTeacher(Long id) {

        return teacherRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Teacher profile not found"
                ));
    }

    private Section getSection(Long id) {

        return sectionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Section not found"
                ));
    }

    private Subject getSubject(Long id) {

        return subjectRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Subject not found"
                ));
    }

    private TeacherAssignment getAssignment(Long id) {

        return assignmentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Teacher assignment not found"
                ));
    }

    private void validateTeacherAndSubject(
            TeacherProfile teacher,
            Subject subject
    ) {

        if (!teacher.isActive()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Cannot assign an inactive teacher"
            );
        }

        if (!subject.isActive()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Cannot assign an inactive subject"
            );
        }
    }

    private TeacherAssignmentResponse toResponse(
            TeacherAssignment assignment
    ) {

        Section section = assignment.getSection();
        Subject subject = assignment.getSubject();
        TeacherProfile teacher = assignment.getTeacher();

        return new TeacherAssignmentResponse(
                assignment.getId(),

                teacher.getId(),
                teacher.getEmployeeCode(),

                section.getId(),
                section.getSectionName(),
                section.getSchoolClass().getName(),
                section.getAcademicYear(),

                subject.getId(),
                subject.getSubjectCode(),
                subject.getName(),

                assignment.getAssignedOn(),
                assignment.isActive()
        );
    }
}