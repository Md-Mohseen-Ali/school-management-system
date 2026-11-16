package com.schoolmanagement.modules.examination.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
import java.util.List;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.schoolmanagement.modules.examination.entity.Examination;
import com.schoolmanagement.modules.examination.entity.ExaminationStatus;

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
    
    /**
     * Published exams are visible when a student was enrolled in the exam's
     * section on at least one configured subject date. Ended enrollments remain
     * valid for historical results.
     */
    @Query("""
        SELECT DISTINCT e
        FROM Examination e
        WHERE e.status = :status
          AND EXISTS (
              SELECT es.id
              FROM ExaminationSubject es, StudentEnrollment enrollment
              WHERE es.examination = e
                AND enrollment.section = e.section
                AND enrollment.student.id IN :studentIds
                AND enrollment.startDate <= es.examinationDate
                AND (enrollment.endDate IS NULL
                     OR enrollment.endDate >= es.examinationDate)
          )
        ORDER BY e.startDate DESC
        """)
    List<Examination> findPublishedForStudents(
            @Param("studentIds") List<Long> studentIds,
            @Param("status") ExaminationStatus status
    );

    @Query("""
        SELECT DISTINCT e
        FROM Examination e
        WHERE EXISTS (
            SELECT es.id
            FROM ExaminationSubject es, TeacherAssignment assignment
            WHERE es.examination = e
              AND assignment.section = e.section
              AND assignment.subject = es.subject
              AND assignment.teacher.id = :teacherId
              AND assignment.active = TRUE
        )
        ORDER BY e.startDate DESC
        """)
    List<Examination> findExaminationsForTeacher(@Param("teacherId") Long teacherId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT e
        FROM Examination e
        WHERE e.id = :id
        """)
    Optional<Examination> findByIdForUpdate(@Param("id") Long id);
}