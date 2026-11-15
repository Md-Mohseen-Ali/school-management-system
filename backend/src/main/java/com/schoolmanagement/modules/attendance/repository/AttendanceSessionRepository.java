package com.schoolmanagement.modules.attendance.repository;

import java.time.LocalDate;
import java.util.Optional;
import java.time.LocalDate;

import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.schoolmanagement.modules.attendance.entity.AttendanceSession;

public interface AttendanceSessionRepository extends
        JpaRepository<AttendanceSession, Long>,
        JpaSpecificationExecutor<AttendanceSession> {

    boolean existsBySection_IdAndAttendanceDateAndSessionCode(
            Long sectionId,
            LocalDate attendanceDate,
            String sessionCode
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT s FROM AttendanceSession s
        WHERE s.id = :id
        """)
    Optional<AttendanceSession> findByIdForUpdate(
            @Param("id") Long id
    );
    
    
    @Query("""
    	    SELECT COUNT(sess)
    	    FROM AttendanceSession sess
    	    WHERE sess.section.id = :sectionId
    	      AND sess.attendanceDate BETWEEN :fromDate AND :toDate
    	      AND sess.status IN (
    	          com.schoolmanagement.modules.attendance.entity.AttendanceSessionStatus.SUBMITTED,
    	          com.schoolmanagement.modules.attendance.entity.AttendanceSessionStatus.LOCKED
    	      )
    	    """)
    	long countFinalSessions(
    	        @Param("sectionId") Long sectionId,
    	        @Param("fromDate") LocalDate fromDate,
    	        @Param("toDate") LocalDate toDate
    	);
}