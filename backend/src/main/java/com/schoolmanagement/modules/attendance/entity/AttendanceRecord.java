package com.schoolmanagement.modules.attendance.entity;

import java.time.Instant;

import jakarta.persistence.*;

import com.schoolmanagement.modules.academic.entity.Student;

@Entity
@Table(
    name = "attendance_records",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_attendance_student_session",
        columnNames = {"attendance_session_id", "student_id"}
    )
)
public class AttendanceRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "attendance_session_id", nullable = false)
    private AttendanceSession attendanceSession;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Enumerated(EnumType.STRING)
    @Column(name = "attendance_status", nullable = false, length = 15)
    private AttendanceRecordStatus attendanceStatus;

    @Column(name = "remarks", length = 255)
    private String remarks;

    @Column(name = "marked_at", nullable = false)
    private Instant markedAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected AttendanceRecord() {
    }

    public AttendanceRecord(
            AttendanceSession attendanceSession,
            Student student,
            AttendanceRecordStatus attendanceStatus,
            String remarks
    ) {
        this.attendanceSession = attendanceSession;
        this.student = student;
        this.attendanceStatus = attendanceStatus;
        this.remarks = remarks;
    }

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        this.markedAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public AttendanceSession getAttendanceSession() {
        return attendanceSession;
    }

    public Student getStudent() {
        return student;
    }

    public AttendanceRecordStatus getAttendanceStatus() {
        return attendanceStatus;
    }

    public String getRemarks() {
        return remarks;
    }

    public Instant getMarkedAt() {
        return markedAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void update(
            AttendanceRecordStatus attendanceStatus,
            String remarks
    ) {
        this.attendanceStatus = attendanceStatus;
        this.remarks = remarks;
    }
}