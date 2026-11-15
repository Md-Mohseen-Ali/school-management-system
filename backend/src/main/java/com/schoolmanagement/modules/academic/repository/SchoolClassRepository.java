package com.schoolmanagement.modules.academic.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.schoolmanagement.modules.academic.entity.SchoolClass;

public interface SchoolClassRepository
        extends JpaRepository<SchoolClass, Long> {

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    Page<SchoolClass> findByNameContainingIgnoreCase(
            String name,
            Pageable pageable
    );
}