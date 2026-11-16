package com.schoolmanagement.modules.academic.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.schoolmanagement.modules.academic.dto.StudentSubjectTeacherResponse;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.schoolmanagement.modules.academic.entity.TeacherAssignment;

public interface TeacherAssignmentRepository extends
        JpaRepository<TeacherAssignment, Long>,
        JpaSpecificationExecutor<TeacherAssignment> {

    java.util.List<TeacherAssignment> findByTeacher_IdAndActiveTrue(Long teacherId);

    boolean existsByTeacher_IdAndSection_IdAndSubject_IdAndActiveTrue(
            Long teacherId,
            Long sectionId,
            Long subjectId
    );

    boolean existsByTeacher_IdAndSection_IdAndSubject_IdAndActiveTrueAndIdNot(
            Long teacherId,
            Long sectionId,
            Long subjectId,
            Long id
    );
    
    boolean existsByTeacher_IdAndSection_IdAndActiveTrue(
            Long teacherId,
            Long sectionId
    );

    @Query("""
            SELECT new com.schoolmanagement.modules.academic.dto.StudentSubjectTeacherResponse(
                a.subject.id, a.subject.subjectCode, a.subject.name,
                u.fullName, u.email, a.assignedOn
            )
            FROM TeacherAssignment a, com.schoolmanagement.modules.identity.entity.AppUser u
            WHERE a.section.id = :sectionId
              AND a.active = TRUE
              AND a.teacher.active = TRUE
              AND a.subject.active = TRUE
              AND u.id = a.teacher.userId
              AND u.enabled = TRUE
            ORDER BY a.subject.name ASC
            """)
    java.util.List<StudentSubjectTeacherResponse> findStudentSubjectsAndTeachers(
            @Param("sectionId") Long sectionId
    );
}