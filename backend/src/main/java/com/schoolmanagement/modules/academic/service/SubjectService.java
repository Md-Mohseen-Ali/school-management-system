package com.schoolmanagement.modules.academic.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import jakarta.persistence.criteria.Predicate;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.schoolmanagement.modules.academic.dto.SubjectRequest;
import com.schoolmanagement.modules.academic.dto.SubjectResponse;
import com.schoolmanagement.modules.academic.entity.Subject;
import com.schoolmanagement.modules.academic.repository.SubjectRepository;

@Service
public class SubjectService {

    private final SubjectRepository subjectRepository;

    public SubjectService(SubjectRepository subjectRepository) {
        this.subjectRepository = subjectRepository;
    }

    @Transactional
    public SubjectResponse createSubject(SubjectRequest request) {

        String subjectCode = normalizeCode(request.subjectCode());
        String name = normalizeName(request.name());

        validateDuplicates(subjectCode, name);

        Subject subject = new Subject(subjectCode, name);

        return toResponse(subjectRepository.save(subject));
    }

    @Transactional(readOnly = true)
    public Page<SubjectResponse> getAllSubjects(
            String search,
            Boolean active,
            Pageable pageable
    ) {

        Specification<Subject> specification = (root, query, cb) -> {

            List<Predicate> predicates = new ArrayList<>();

            if (active != null) {
                predicates.add(
                        cb.equal(root.get("active"), active)
                );
            }

            if (search != null && !search.isBlank()) {

                String pattern = "%"
                        + search.trim().toLowerCase(Locale.ROOT)
                        + "%";

                Predicate codeMatch = cb.like(
                        cb.lower(root.<String>get("subjectCode")),
                        pattern
                );

                Predicate nameMatch = cb.like(
                        cb.lower(root.<String>get("name")),
                        pattern
                );

                predicates.add(cb.or(codeMatch, nameMatch));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return subjectRepository.findAll(specification, pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public SubjectResponse getSubjectById(Long id) {

        Subject subject = findSubject(id);

        return toResponse(subject);
    }

    @Transactional
    public SubjectResponse updateSubject(
            Long id,
            SubjectRequest request
    ) {

        Subject subject = findSubject(id);

        String subjectCode = normalizeCode(request.subjectCode());
        String name = normalizeName(request.name());

        if (subjectRepository.existsBySubjectCodeIgnoreCaseAndIdNot(
                subjectCode, id)) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Another subject already uses this subject code"
            );
        }

        if (subjectRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Another subject already uses this name"
            );
        }

        subject.setSubjectCode(subjectCode);
        subject.setName(name);

        return toResponse(subjectRepository.save(subject));
    }

    @Transactional
    public SubjectResponse updateSubjectStatus(
            Long id,
            boolean active
    ) {

        Subject subject = findSubject(id);

        subject.setActive(active);

        return toResponse(subjectRepository.save(subject));
    }

    private void validateDuplicates(String subjectCode, String name) {

        if (subjectRepository.existsBySubjectCodeIgnoreCase(subjectCode)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Subject code already exists"
            );
        }

        if (subjectRepository.existsByNameIgnoreCase(name)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Subject name already exists"
            );
        }
    }

    private Subject findSubject(Long id) {

        return subjectRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Subject not found"
                ));
    }

    private String normalizeCode(String subjectCode) {
        return subjectCode.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeName(String name) {
        return name.trim();
    }

    private SubjectResponse toResponse(Subject subject) {

        return new SubjectResponse(
                subject.getId(),
                subject.getSubjectCode(),
                subject.getName(),
                subject.isActive()
        );
    }
}