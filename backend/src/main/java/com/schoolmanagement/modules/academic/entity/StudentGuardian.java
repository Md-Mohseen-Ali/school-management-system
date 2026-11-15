package com.schoolmanagement.modules.academic.entity;

import jakarta.persistence.*;

import com.schoolmanagement.modules.identity.entity.GuardianProfile;

@Entity
@Table(name = "student_guardians")
public class StudentGuardian {

    @EmbeddedId
    private StudentGuardianId id;

    @MapsId("studentId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @MapsId("guardianId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "guardian_id", nullable = false)
    private GuardianProfile guardian;

    @Column(name = "relationship", nullable = false, length = 30)
    private String relationship;

    @Column(name = "is_primary", nullable = false)
    private boolean primaryGuardian = false;

    protected StudentGuardian() {
    }

    public StudentGuardian(
            Student student,
            GuardianProfile guardian,
            String relationship,
            boolean primaryGuardian
    ) {
        this.id = new StudentGuardianId(
                student.getId(),
                guardian.getId()
        );

        this.student = student;
        this.guardian = guardian;
        this.relationship = relationship;
        this.primaryGuardian = primaryGuardian;
    }

    public StudentGuardianId getId() {
        return id;
    }

    public Student getStudent() {
        return student;
    }

    public GuardianProfile getGuardian() {
        return guardian;
    }

    public String getRelationship() {
        return relationship;
    }

    public boolean isPrimaryGuardian() {
        return primaryGuardian;
    }

    public void setPrimaryGuardian(boolean primaryGuardian) {
        this.primaryGuardian = primaryGuardian;
    }
}