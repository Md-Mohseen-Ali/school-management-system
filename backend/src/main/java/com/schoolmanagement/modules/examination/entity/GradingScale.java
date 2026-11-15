package com.schoolmanagement.modules.examination.entity;

import java.math.BigDecimal;

import jakarta.persistence.*;

@Entity
@Table(
    name = "grading_scales",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_grade_academic_year",
        columnNames = {"academic_year", "grade"}
    )
)
public class GradingScale {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "academic_year", nullable = false, length = 9)
    private String academicYear;

    @Column(name = "grade", nullable = false, length = 5)
    private String grade;

    @Column(name = "min_percentage", nullable = false, precision = 5, scale = 2)
    private BigDecimal minPercentage;

    @Column(name = "max_percentage", nullable = false, precision = 5, scale = 2)
    private BigDecimal maxPercentage;

    @Column(name = "remarks", length = 100)
    private String remarks;

    protected GradingScale() {
    }

    public GradingScale(
            String academicYear,
            String grade,
            BigDecimal minPercentage,
            BigDecimal maxPercentage,
            String remarks
    ) {
        this.academicYear = academicYear;
        this.grade = grade;
        this.minPercentage = minPercentage;
        this.maxPercentage = maxPercentage;
        this.remarks = remarks;
    }

    public Long getId() {
        return id;
    }

    public String getAcademicYear() {
        return academicYear;
    }

    public String getGrade() {
        return grade;
    }

    public BigDecimal getMinPercentage() {
        return minPercentage;
    }

    public BigDecimal getMaxPercentage() {
        return maxPercentage;
    }

    public String getRemarks() {
        return remarks;
    }
}