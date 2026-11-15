package com.schoolmanagement.modules.academic.repository;
import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.schoolmanagement.modules.academic.entity.StudentEnrollment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.schoolmanagement.modules.academic.entity.StudentEnrollment;

public interface StudentEnrollmentRepository extends
        JpaRepository<StudentEnrollment, Long>,
        JpaSpecificationExecutor<StudentEnrollment> {

    boolean existsByStudent_IdAndEndDateIsNull(Long studentId);
    
    @Query("""
    	    SELECT CASE WHEN COUNT(e) > 0 THEN TRUE ELSE FALSE END
    	    FROM StudentEnrollment e
    	    WHERE e.student.id = :studentId
    	      AND e.section.id = :sectionId
    	      AND e.startDate <= :attendanceDate
    	      AND (e.endDate IS NULL OR e.endDate >= :attendanceDate)
    	    """)
    	boolean existsEnrollmentOnDate(
    	        @Param("studentId") Long studentId,
    	        @Param("sectionId") Long sectionId,
    	        @Param("attendanceDate") LocalDate attendanceDate
    	);

    	@Query("""
    	    SELECT e
    	    FROM StudentEnrollment e
    	    WHERE e.section.id = :sectionId
    	      AND e.startDate <= :attendanceDate
    	      AND (e.endDate IS NULL OR e.endDate >= :attendanceDate)
    	    """)
    	List<StudentEnrollment> findEnrollmentsOnDate(
    	        @Param("sectionId") Long sectionId,
    	        @Param("attendanceDate") LocalDate attendanceDate
    	);
}