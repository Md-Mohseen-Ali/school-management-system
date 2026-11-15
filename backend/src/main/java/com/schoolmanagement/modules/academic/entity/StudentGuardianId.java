package com.schoolmanagement.modules.academic.entity;

import java.io.Serializable;
import java.util.Objects;

import jakarta.persistence.Embeddable;

@Embeddable
public class StudentGuardianId implements Serializable {

    private Long studentId;
    private Long guardianId;

    public StudentGuardianId() {
    }

    public StudentGuardianId(Long studentId, Long guardianId) {
        this.studentId = studentId;
        this.guardianId = guardianId;
    }

    public Long getStudentId() {
        return studentId;
    }

    public Long getGuardianId() {
        return guardianId;
    }

    @Override
    public boolean equals(Object object) {

        if (this == object) return true;

        if (!(object instanceof StudentGuardianId other)) {
            return false;
        }

        return Objects.equals(studentId, other.studentId)
                && Objects.equals(guardianId, other.guardianId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(studentId, guardianId);
    }
}