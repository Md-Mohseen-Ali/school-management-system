package com.schoolmanagement.modules.identity.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.schoolmanagement.modules.identity.entity.TeacherProfile;

public interface TeacherProfileRepository
        extends JpaRepository<TeacherProfile, Long> {

    List<TeacherProfile> findByActiveTrueOrderByEmployeeCodeAsc();

    boolean existsByEmployeeCodeIgnoreCase(String employeeCode);

    Optional<TeacherProfile> findByUserId(Long userId);

    @Query("""
        SELECT t
        FROM TeacherProfile t, AppUser u
        WHERE t.userId = u.id
          AND LOWER(u.email) = LOWER(:email)
          AND t.active = TRUE
        """)
    Optional<TeacherProfile> findActiveByUserEmail(
            @Param("email") String email
    );
}