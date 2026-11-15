package com.schoolmanagement.modules.examination.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.schoolmanagement.modules.examination.entity.GradingScale;

public interface GradingScaleRepository
        extends JpaRepository<GradingScale, Long> {

    boolean existsByAcademicYearAndGrade(
            String academicYear,
            String grade
    );

    List<GradingScale>
    findByAcademicYearOrderByMinPercentageDesc(String academicYear);
}