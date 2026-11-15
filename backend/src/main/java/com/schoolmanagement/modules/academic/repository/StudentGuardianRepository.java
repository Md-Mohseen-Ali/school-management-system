package com.schoolmanagement.modules.academic.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.schoolmanagement.modules.academic.entity.StudentGuardian;
import com.schoolmanagement.modules.academic.entity.StudentGuardianId;

public interface StudentGuardianRepository extends
        JpaRepository<StudentGuardian, StudentGuardianId>,
        JpaSpecificationExecutor<StudentGuardian> {

    Optional<StudentGuardian> findByStudent_IdAndGuardian_Id(
            Long studentId,
            Long guardianId
    );

    List<StudentGuardian> findByStudent_Id(Long studentId);

    List<StudentGuardian> findByGuardian_Id(Long guardianId);
    
    boolean existsByStudent_IdAndGuardian_Id(
            Long studentId,
            Long guardianId
    );
}