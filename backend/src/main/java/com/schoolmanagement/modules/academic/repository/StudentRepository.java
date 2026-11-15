package com.schoolmanagement.modules.academic.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.schoolmanagement.modules.academic.entity.Student;

public interface StudentRepository extends
        JpaRepository<Student, Long>,
        JpaSpecificationExecutor<Student> {

    boolean existsByAdmissionNumberIgnoreCase(String admissionNumber);
}