package com.schoolmanagement.modules.academic.entity;

import java.time.LocalDate;

import jakarta.persistence.*;

import com.schoolmanagement.modules.identity.entity.TeacherProfile;

@Entity
@Table(name = "teacher_assignments")
public class TeacherAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "teacher_id", nullable = false)
    private TeacherProfile teacher;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "section_id", nullable = false)
    private Section section;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;

    @Column(name = "assigned_on", nullable = false)
    private LocalDate assignedOn;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    protected TeacherAssignment() {
    }

    public TeacherAssignment(
            TeacherProfile teacher,
            Section section,
            Subject subject
    ) {
        this.teacher = teacher;
        this.section = section;
        this.subject = subject;
        this.assignedOn = LocalDate.now();
        this.active = true;
    }

    public Long getId() {
        return id;
    }

    public TeacherProfile getTeacher() {
        return teacher;
    }

    public Section getSection() {
        return section;
    }

    public Subject getSubject() {
        return subject;
    }

    public LocalDate getAssignedOn() {
        return assignedOn;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}