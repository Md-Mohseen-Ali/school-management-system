package com.schoolmanagement.modules.academic.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "subjects")
public class Subject {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "subject_code", nullable = false, length = 30, unique = true)
    private String subjectCode;

    @Column(name = "name", nullable = false, length = 100, unique = true)
    private String name;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    protected Subject() {
    }

    public Subject(String subjectCode, String name) {
        this.subjectCode = subjectCode;
        this.name = name;
        this.active = true;
    }

    public Long getId() {
        return id;
    }

    public String getSubjectCode() {
        return subjectCode;
    }

    public void setSubjectCode(String subjectCode) {
        this.subjectCode = subjectCode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}