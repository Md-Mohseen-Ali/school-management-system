package com.schoolmanagement.modules.academic.repository;

import org.springframework.data.jpa.repository.JpaRepository;
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
}