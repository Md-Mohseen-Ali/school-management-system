package com.schoolmanagement.modules.academic.entity;

import jakarta.persistence.*;

@Entity
@Table(
    name = "sections",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_section_year",
        columnNames = {"school_class_id", "section_name", "academic_year"}
    )
)
public class Section {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "school_class_id", nullable = false)
    private SchoolClass schoolClass;

    @Column(name = "section_name", nullable = false, length = 20)
    private String sectionName;

    @Column(name = "academic_year", nullable = false, length = 9)
    private String academicYear;

    protected Section() {
    }

    public Section(
            SchoolClass schoolClass,
            String sectionName,
            String academicYear
    ) {
        this.schoolClass = schoolClass;
        this.sectionName = sectionName;
        this.academicYear = academicYear;
    }

    public Long getId() {
        return id;
    }

    public SchoolClass getSchoolClass() {
        return schoolClass;
    }

    public void setSchoolClass(SchoolClass schoolClass) {
        this.schoolClass = schoolClass;
    }

    public String getSectionName() {
        return sectionName;
    }

    public void setSectionName(String sectionName) {
        this.sectionName = sectionName;
    }

    public String getAcademicYear() {
        return academicYear;
    }

    public void setAcademicYear(String academicYear) {
        this.academicYear = academicYear;
    }
}