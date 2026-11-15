package com.schoolmanagement.modules.attendance.entity;

import java.time.Instant;
import java.time.LocalDate;

import jakarta.persistence.*;

import com.schoolmanagement.modules.academic.entity.Section;
import com.schoolmanagement.modules.identity.entity.TeacherProfile;

@Entity
@Table(
    name = "attendance_sessions",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_attendance_section_date_session",
        columnNames = {"section_id", "attendance_date", "session_code"}
    )
)
public class AttendanceSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "section_id", nullable = false)
    private Section section;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "marked_by_teacher_id", nullable = false)
    private TeacherProfile markedByTeacher;

    @Column(name = "attendance_date", nullable = false)
    private LocalDate attendanceDate;

    @Column(name = "session_code", nullable = false, length = 20)
    private String sessionCode = "FULL_DAY";

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 15)
    private AttendanceSessionStatus status = AttendanceSessionStatus.DRAFT;

    @Column(name = "notes", length = 255)
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    protected AttendanceSession() {
    }
    
    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }

    public AttendanceSession(
            Section section,
            TeacherProfile markedByTeacher,
            LocalDate attendanceDate,
            String sessionCode,
            String notes
    ) {
        this.section = section;
        this.markedByTeacher = markedByTeacher;
        this.attendanceDate = attendanceDate;
        this.sessionCode = sessionCode;
        this.notes = notes;
        this.status = AttendanceSessionStatus.DRAFT;
    }

    public Long getId() {
        return id;
    }

    public Section getSection() {
        return section;
    }

    public TeacherProfile getMarkedByTeacher() {
        return markedByTeacher;
    }

    public LocalDate getAttendanceDate() {
        return attendanceDate;
    }

    public String getSessionCode() {
        return sessionCode;
    }

    public AttendanceSessionStatus getStatus() {
        return status;
    }

    public String getNotes() {
        return notes;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getSubmittedAt() {
        return submittedAt;
    }

    public void submit() {
        this.status = AttendanceSessionStatus.SUBMITTED;
        this.submittedAt = Instant.now();
    }

    public void lock() {
        this.status = AttendanceSessionStatus.LOCKED;
    }
}