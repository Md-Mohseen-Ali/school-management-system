package com.schoolmanagement.modules.academic.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.schoolmanagement.modules.academic.dto.SchoolClassRequest;
import com.schoolmanagement.modules.academic.dto.SchoolClassResponse;
import com.schoolmanagement.modules.academic.entity.SchoolClass;
import com.schoolmanagement.modules.academic.repository.SchoolClassRepository;

@Service
public class SchoolClassService {

    private final SchoolClassRepository schoolClassRepository;

    public SchoolClassService(SchoolClassRepository schoolClassRepository) {
        this.schoolClassRepository = schoolClassRepository;
    }

    @Transactional
    public SchoolClassResponse createClass(SchoolClassRequest request) {

        String name = normalizeName(request.name());

        if (schoolClassRepository.existsByNameIgnoreCase(name)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "A class with this name already exists"
            );
        }

        SchoolClass schoolClass = new SchoolClass(
                name,
                normalizeDescription(request.description())
        );

        SchoolClass saved = schoolClassRepository.save(schoolClass);

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public Page<SchoolClassResponse> getAllClasses(
            String search,
            Pageable pageable
    ) {

        Page<SchoolClass> classes;

        if (search == null || search.isBlank()) {
            classes = schoolClassRepository.findAll(pageable);
        } else {
            classes = schoolClassRepository
                    .findByNameContainingIgnoreCase(search.trim(), pageable);
        }

        return classes.map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public SchoolClassResponse getClassById(Long id) {

        SchoolClass schoolClass = schoolClassRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Class not found"
                ));

        return toResponse(schoolClass);
    }

    @Transactional
    public SchoolClassResponse updateClass(
            Long id,
            SchoolClassRequest request
    ) {

        SchoolClass schoolClass = schoolClassRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Class not found"
                ));

        String name = normalizeName(request.name());

        if (schoolClassRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Another class with this name already exists"
            );
        }

        schoolClass.setName(name);
        schoolClass.setDescription(
                normalizeDescription(request.description())
        );

        return toResponse(schoolClassRepository.save(schoolClass));
    }

    private String normalizeName(String name) {
        return name.trim();
    }

    private String normalizeDescription(String description) {
        if (description == null || description.isBlank()) {
            return null;
        }

        return description.trim();
    }

    private SchoolClassResponse toResponse(SchoolClass schoolClass) {
        return new SchoolClassResponse(
                schoolClass.getId(),
                schoolClass.getName(),
                schoolClass.getDescription()
        );
    }
}