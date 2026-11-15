package com.schoolmanagement.modules.examination.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.schoolmanagement.modules.examination.entity.Examination;

public interface ExaminationRepository extends
        JpaRepository<Examination, Long>,
        JpaSpecificationExecutor<Examination> {

    boolean existsBySection_IdAndExamNameAndAcademicYear(
            Long sectionId,
            String examName,
            String academicYear
    );

    java.util.List<Examination> findBySection_IdInAndStatusOrderByStartDateDesc(
            java.util.List<Long> sectionIds,
            com.schoolmanagement.modules.examination.entity.ExaminationStatus status
    );
    
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT e
        FROM Examination e
        WHERE e.id = :id
        """)
    Optional<Examination> findByIdForUpdate(@Param("id") Long id);
}