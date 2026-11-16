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

import com.schoolmanagement.modules.academic.dto.StudentGuardianRequest;
import com.schoolmanagement.modules.academic.dto.StudentGuardianResponse;
import com.schoolmanagement.modules.academic.entity.Student;
import com.schoolmanagement.modules.academic.entity.StudentGuardian;
import com.schoolmanagement.modules.academic.repository.StudentGuardianRepository;
import com.schoolmanagement.modules.academic.repository.StudentRepository;

import com.schoolmanagement.modules.identity.entity.GuardianProfile;
import com.schoolmanagement.modules.identity.repository.GuardianProfileRepository;

@Service
public class StudentGuardianService {

    private final StudentGuardianRepository linkRepository;
    private final StudentRepository studentRepository;
    private final GuardianProfileRepository guardianRepository;
    private final AuditService auditService;

    public StudentGuardianService(
            StudentGuardianRepository linkRepository,
            StudentRepository studentRepository,
            GuardianProfileRepository guardianRepository,
            AuditService auditService
    ) {
        this.linkRepository = linkRepository;
        this.studentRepository = studentRepository;
        this.guardianRepository = guardianRepository;
        this.auditService = auditService;
    }

    @Transactional
    public StudentGuardianResponse linkGuardian(
            StudentGuardianRequest request
    ) {

        Student student = studentRepository.findById(request.studentId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Student not found"
                ));

        GuardianProfile guardian = guardianRepository
                .findById(request.guardianId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Guardian profile not found"
                ));

        if (linkRepository.findByStudent_IdAndGuardian_Id(
                student.getId(), guardian.getId()).isPresent()) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "This guardian is already linked to the student"
            );
        }

        if (request.primary()) {
            clearExistingPrimary(student.getId());
        }

        StudentGuardian link = new StudentGuardian(
                student,
                guardian,
                request.relationship().trim(),
                request.primary()
        );

        try {
            StudentGuardian saved = linkRepository.saveAndFlush(link);
            auditService.record("STUDENT_GUARDIAN_LINKED", "STUDENT_GUARDIAN",
                    student.getId() + ":" + guardian.getId(), null,
                    Map.of("studentId", student.getId(), "guardianId", guardian.getId(),
                            "relationship", saved.getRelationship(), "primary", saved.isPrimaryGuardian()));
            return toResponse(saved);
        } catch (DataIntegrityViolationException exception) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Guardian link conflicts with an existing relationship",
                    exception
            );
        }
    }

    @Transactional(readOnly = true)
    public Page<StudentGuardianResponse> getAllLinks(
            Long studentId,
            Long guardianId,
            Pageable pageable
    ) {

        Specification<StudentGuardian> specification =
                (root, query, cb) -> {

            List<Predicate> predicates = new ArrayList<>();

            if (studentId != null) {
                predicates.add(
                        cb.equal(root.get("student").get("id"), studentId)
                );
            }

            if (guardianId != null) {
                predicates.add(
                        cb.equal(root.get("guardian").get("id"), guardianId)
                );
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return linkRepository.findAll(specification, pageable)
                .map(this::toResponse);
    }

    @Transactional
    public StudentGuardianResponse updatePrimaryStatus(
            Long studentId,
            Long guardianId,
            boolean primary
    ) {

        StudentGuardian link = findLink(studentId, guardianId);

        if (primary) {
            clearExistingPrimary(studentId);
        }

        boolean oldPrimary = link.isPrimaryGuardian();
        link.setPrimaryGuardian(primary);

        try {
            StudentGuardian saved = linkRepository.saveAndFlush(link);
            auditService.record("STUDENT_GUARDIAN_PRIMARY_CHANGED", "STUDENT_GUARDIAN",
                    studentId + ":" + guardianId, Map.of("primary", oldPrimary),
                    Map.of("primary", saved.isPrimaryGuardian()));
            return toResponse(saved);
        } catch (DataIntegrityViolationException exception) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Another primary guardian is already assigned",
                    exception
            );
        }
    }

    @Transactional
    public void unlinkGuardian(Long studentId, Long guardianId) {

        StudentGuardian link = findLink(studentId, guardianId);
        auditService.record("STUDENT_GUARDIAN_UNLINKED", "STUDENT_GUARDIAN",
                studentId + ":" + guardianId, Map.of("primary", link.isPrimaryGuardian()),
                Map.of("linked", false));
        linkRepository.delete(link);
    }

    private void clearExistingPrimary(Long studentId) {

        List<StudentGuardian> existingLinks =
                linkRepository.findByStudent_Id(studentId);

        for (StudentGuardian link : existingLinks) {
            link.setPrimaryGuardian(false);
        }

        linkRepository.saveAll(existingLinks);

        // Clear the previous primary before assigning a new one.
        linkRepository.flush();
    }

    private StudentGuardian findLink(
            Long studentId,
            Long guardianId
    ) {

        return linkRepository
                .findByStudent_IdAndGuardian_Id(studentId, guardianId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Guardian relationship not found"
                ));
    }

    private StudentGuardianResponse toResponse(
            StudentGuardian link
    ) {

        Student student = link.getStudent();
        GuardianProfile guardian = link.getGuardian();

        return new StudentGuardianResponse(

                student.getId(),
                student.getAdmissionNumber(),
                student.getFirstName() + " " + student.getLastName(),

                guardian.getId(),
                guardian.getUserId(),
                guardian.getAlternatePhone(),

                link.getRelationship(),
                link.isPrimaryGuardian()
        );
    }
}