package com.schoolmanagement.modules.identity.entity;

import java.time.LocalDate;

import jakarta.persistence.*;

@Entity
@Table(name = "teacher_profiles")
public class TeacherProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(name = "employee_code", nullable = false, unique = true, length = 30)
    private String employeeCode;

    @Column(name = "hire_date")
    private LocalDate hireDate;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    protected TeacherProfile() {
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public String getEmployeeCode() {
        return employeeCode;
    }

    public LocalDate getHireDate() {
        return hireDate;
    }

    public boolean isActive() {
        return active;
    }
}