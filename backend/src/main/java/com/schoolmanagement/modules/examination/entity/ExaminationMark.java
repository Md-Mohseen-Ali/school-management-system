package com.schoolmanagement.modules.examination.entity;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.persistence.*;

import com.schoolmanagement.modules.academic.entity.Student;

@Entity
@Table(
    name = "examination_marks",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_student_exam_subject",
        columnNames = {"examination_subject_id", "student_id"}
    )
)
public class ExaminationMark {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "examination_subject_id", nullable = false)
    private ExaminationSubject examinationSubject;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Column(name = "marks_obtained", nullable = false, precision = 7, scale = 2)
    private BigDecimal marksObtained;

    @Enumerated(EnumType.STRING)
    @Column(name = "attendance_status", nullable = false, length = 15)
    private ExamAttendanceStatus attendanceStatus = ExamAttendanceStatus.PRESENT;

    @Column(name = "remarks", length = 255)
    private String remarks;

    @Column(name = "entered_at", nullable = false, updatable = false)
    private Instant enteredAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ExaminationMark() {
    }

    public ExaminationMark(
            ExaminationSubject examinationSubject,
            Student student,
            BigDecimal marksObtained,
            ExamAttendanceStatus attendanceStatus,
            String remarks
    ) {
        this.examinationSubject = examinationSubject;
        this.student = student;
        this.marksObtained = marksObtained;
        this.attendanceStatus = attendanceStatus;
        this.remarks = remarks;
    }

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        this.enteredAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public ExaminationSubject getExaminationSubject() {
        return examinationSubject;
    }

    public Student getStudent() {
        return student;
    }

    public BigDecimal getMarksObtained() {
        return marksObtained;
    }

    public ExamAttendanceStatus getAttendanceStatus() {
        return attendanceStatus;
    }

    public String getRemarks() {
        return remarks;
    }

    public Instant getEnteredAt() {
        return enteredAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void update(
            BigDecimal marksObtained,
            ExamAttendanceStatus attendanceStatus,
            String remarks
    ) {
        this.marksObtained = marksObtained;
        this.attendanceStatus = attendanceStatus;
        this.remarks = remarks;
    }
}