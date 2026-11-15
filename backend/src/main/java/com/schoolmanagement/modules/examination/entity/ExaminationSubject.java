package com.schoolmanagement.modules.examination.entity;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.persistence.*;

import com.schoolmanagement.modules.academic.entity.Subject;

@Entity
@Table(
    name = "examination_subjects",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_examination_subject",
        columnNames = {"examination_id", "subject_id"}
    )
)
public class ExaminationSubject {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "examination_id", nullable = false)
    private Examination examination;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;

    @Column(name = "examination_date", nullable = false)
    private LocalDate examinationDate;

    @Column(name = "max_marks", nullable = false, precision = 7, scale = 2)
    private BigDecimal maxMarks;

    @Column(name = "pass_marks", nullable = false, precision = 7, scale = 2)
    private BigDecimal passMarks;

    protected ExaminationSubject() {
    }

    // Required constructor
    public ExaminationSubject(
            Examination examination,
            Subject subject,
            LocalDate examinationDate,
            BigDecimal maxMarks,
            BigDecimal passMarks
    ) {
        this.examination = examination;
        this.subject = subject;
        this.examinationDate = examinationDate;
        this.maxMarks = maxMarks;
        this.passMarks = passMarks;
    }

    public Long getId() {
        return id;
    }

    public Examination getExamination() {
        return examination;
    }

    public Subject getSubject() {
        return subject;
    }

    public LocalDate getExaminationDate() {
        return examinationDate;
    }

    public BigDecimal getMaxMarks() {
        return maxMarks;
    }

    public BigDecimal getPassMarks() {
        return passMarks;
    }

    public void updateConfiguration(
            LocalDate examinationDate,
            BigDecimal maxMarks,
            BigDecimal passMarks
    ) {
        this.examinationDate = examinationDate;
        this.maxMarks = maxMarks;
        this.passMarks = passMarks;
    }
}