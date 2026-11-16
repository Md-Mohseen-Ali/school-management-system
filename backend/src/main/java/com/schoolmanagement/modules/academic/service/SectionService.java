package com.schoolmanagement.modules.academic.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.schoolmanagement.common.audit.AuditService;

import jakarta.persistence.criteria.Predicate;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.schoolmanagement.modules.academic.dto.SectionRequest;
import com.schoolmanagement.modules.academic.dto.SectionResponse;
import com.schoolmanagement.modules.academic.entity.SchoolClass;
import com.schoolmanagement.modules.academic.entity.Section;
import com.schoolmanagement.modules.academic.repository.SchoolClassRepository;
import com.schoolmanagement.modules.academic.repository.SectionRepository;

@Service
public class SectionService {

    private final SectionRepository sectionRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final AuditService auditService;

    public SectionService(
            SectionRepository sectionRepository,
            SchoolClassRepository schoolClassRepository,
            AuditService auditService
    ) {
        this.sectionRepository = sectionRepository;
        this.schoolClassRepository = schoolClassRepository;
        this.auditService = auditService;
    }

    @Transactional
    public SectionResponse createSection(SectionRequest request) {

        validateAcademicYear(request.academicYear());

        SchoolClass schoolClass = getSchoolClass(request.schoolClassId());

        String sectionName = normalizeSectionName(request.sectionName());

        if (sectionRepository
                .existsBySchoolClass_IdAndSectionNameIgnoreCaseAndAcademicYear(
                        schoolClass.getId(),
                        sectionName,
                        request.academicYear()
                )) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "This section already exists for the selected class and academic year"
            );
        }

        Section section = new Section(
                schoolClass,
                sectionName,
                request.academicYear()
        );

        Section saved = sectionRepository.save(section);
        auditService.record("SECTION_CREATED", "SECTION", saved.getId().toString(),
                null, Map.of("schoolClassId", schoolClass.getId(),
                        "sectionName", saved.getSectionName(),
                        "academicYear", saved.getAcademicYear()));
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public Page<SectionResponse> getAllSections(
            Long classId,
            String academicYear,
            String search,
            Pageable pageable
    ) {

        Specification<Section> specification = (root, query, cb) -> {

            List<Predicate> predicates = new ArrayList<>();

            if (classId != null) {
                predicates.add(
                        cb.equal(root.get("schoolClass").get("id"), classId)
                );
            }

            if (academicYear != null && !academicYear.isBlank()) {
                predicates.add(
                        cb.equal(
                                root.get("academicYear"),
                                academicYear.trim()
                        )
                );
            }

            if (search != null && !search.isBlank()) {
                predicates.add(
                        cb.like(
                                cb.lower(root.get("sectionName")),
                                "%" + search.trim().toLowerCase() + "%"
                        )
                );
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return sectionRepository.findAll(specification, pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public SectionResponse getSectionById(Long id) {

        Section section = sectionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Section not found"
                ));

        return toResponse(section);
    }

    @Transactional
    public SectionResponse updateSection(
            Long id,
            SectionRequest request
    ) {

        validateAcademicYear(request.academicYear());

        Section section = sectionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Section not found"
                ));

        SchoolClass schoolClass = getSchoolClass(request.schoolClassId());

        String sectionName = normalizeSectionName(request.sectionName());

        if (sectionRepository
                .existsBySchoolClass_IdAndSectionNameIgnoreCaseAndAcademicYearAndIdNot(
                        schoolClass.getId(),
                        sectionName,
                        request.academicYear(),
                        id
                )) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Another section with these details already exists"
            );
        }

        String oldSectionName = section.getSectionName();
        String oldAcademicYear = section.getAcademicYear();
        section.setSchoolClass(schoolClass);
        section.setSectionName(sectionName);
        section.setAcademicYear(request.academicYear());

        Section saved = sectionRepository.save(section);
        auditService.record("SECTION_UPDATED", "SECTION", saved.getId().toString(),
                Map.of("sectionName", oldSectionName, "academicYear", oldAcademicYear),
                Map.of("sectionName", saved.getSectionName(), "academicYear", saved.getAcademicYear()));
        return toResponse(saved);
    }

    private SchoolClass getSchoolClass(Long id) {

        return schoolClassRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Class not found"
                ));
    }

    private String normalizeSectionName(String name) {
        return name.trim().toUpperCase();
    }

    private void validateAcademicYear(String academicYear) {

        String[] years = academicYear.split("-");

        int start = Integer.parseInt(years[0]);
        int end = Integer.parseInt(years[1]);

        if (end != start + 1) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Academic year must contain consecutive years"
            );
        }
    }

    private SectionResponse toResponse(Section section) {

        return new SectionResponse(
                section.getId(),
                section.getSchoolClass().getId(),
                section.getSchoolClass().getName(),
                section.getSectionName(),
                section.getAcademicYear()
        );
    }
}