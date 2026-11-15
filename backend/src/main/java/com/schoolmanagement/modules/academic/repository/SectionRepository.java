package com.schoolmanagement.modules.academic.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.schoolmanagement.modules.academic.entity.Section;

public interface SectionRepository extends
        JpaRepository<Section, Long>,
        JpaSpecificationExecutor<Section> {

    boolean existsBySchoolClass_IdAndSectionNameIgnoreCaseAndAcademicYear(
            Long schoolClassId,
            String sectionName,
            String academicYear
    );

    boolean existsBySchoolClass_IdAndSectionNameIgnoreCaseAndAcademicYearAndIdNot(
            Long schoolClassId,
            String sectionName,
            String academicYear,
            Long id
    );
}