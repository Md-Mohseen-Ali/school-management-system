package com.schoolmanagement.modules.academic.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.schoolmanagement.modules.academic.entity.Subject;

public interface SubjectRepository extends
        JpaRepository<Subject, Long>,
        JpaSpecificationExecutor<Subject> {

    boolean existsBySubjectCodeIgnoreCase(String subjectCode);

    boolean existsByNameIgnoreCase(String name);

    boolean existsBySubjectCodeIgnoreCaseAndIdNot(
            String subjectCode,
            Long id
    );

    boolean existsByNameIgnoreCaseAndIdNot(
            String name,
            Long id
    );
}