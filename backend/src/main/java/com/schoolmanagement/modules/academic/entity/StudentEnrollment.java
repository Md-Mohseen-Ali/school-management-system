package com.schoolmanagement.modules.academic.entity;

import java.time.LocalDate;

import jakarta.persistence.*;

import com.schoolmanagement.modules.academic.entity.Student;
import com.schoolmanagement.modules.academic.entity.Section;

@Entity
@Table(
    name = "student_enrollments",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_student_enrollment",
        columnNames = {"student_id", "section_id", "start_date"}
    )
)
public class StudentEnrollment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "section_id", nullable = false)
    private Section section;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    protected StudentEnrollment() {
    }

    public StudentEnrollment(
            Student student,
            Section section,
            LocalDate startDate
    ) {
        this.student = student;
        this.section = section;
        this.startDate = startDate;
    }

    public Long getId() {
        return id;
    }

    public Student getStudent() {
        return student;
    }

    public Section getSection() {
        return section;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public boolean isActive() {
        return endDate == null;
    }
}