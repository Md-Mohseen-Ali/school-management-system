package com.schoolmanagement.modules.examination.entity;

import java.time.Instant;
import java.time.LocalDate;

import jakarta.persistence.*;

import com.schoolmanagement.modules.academic.entity.Section;

@Entity
@Table(
    name = "examinations",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_examination_section",
        columnNames = {"section_id", "exam_name", "academic_year"}
    )
)
public class Examination {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "section_id", nullable = false)
    private Section section;

    @Column(name = "exam_name", nullable = false, length = 100)
    private String examName;

    @Column(name = "academic_year", nullable = false, length = 9)
    private String academicYear;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ExaminationStatus status = ExaminationStatus.DRAFT;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Examination() {
    }

    public Examination(
            Section section,
            String examName,
            String academicYear,
            LocalDate startDate,
            LocalDate endDate
    ) {
        this.section = section;
        this.examName = examName;
        this.academicYear = academicYear;
        this.startDate = startDate;
        this.endDate = endDate;
        this.status = ExaminationStatus.DRAFT;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Section getSection() {
        return section;
    }

    public String getExamName() {
        return examName;
    }

    public String getAcademicYear() {
        return academicYear;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public ExaminationStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void updateDetails(
            String examName,
            LocalDate startDate,
            LocalDate endDate
    ) {
        this.examName = examName;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public void setStatus(ExaminationStatus status) {
        this.status = status;
    }
}