package com.schoolmanagement.modules.identity.entity;

import java.time.Instant;
import java.time.LocalDate;

import jakarta.persistence.*;

@Entity
@Table(name = "account_access_requests")
public class AccountAccessRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "requested_role", nullable = false, length = 20)
    private UserRole requestedRole;

    @Column(name = "full_name", nullable = false, length = 120)
    private String fullName;

    @Column(nullable = false, length = 180)
    private String email;

    @Column(length = 25)
    private String phone;

    @Column(name = "employee_code", length = 30)
    private String employeeCode;

    @Column(name = "hire_date")
    private LocalDate hireDate;

    @Column(name = "alternate_phone", length = 25)
    private String alternatePhone;

    @Column(name = "admission_number", length = 40)
    private String admissionNumber;

    @Column(name = "first_name", length = 80)
    private String firstName;

    @Column(name = "last_name", length = 80)
    private String lastName;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "student_admission_number", length = 40)
    private String studentAdmissionNumber;

    @Column(length = 30)
    private String relationship;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AccountRequestStatus status = AccountRequestStatus.PENDING;

    @Column(name = "review_note", length = 500)
    private String reviewNote;

    @Column(name = "reviewed_by_user_id")
    private Long reviewedByUserId;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @Column(name = "provisioned_entity_id")
    private Long provisionedEntityId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Version
    private long version;

    protected AccountAccessRequest() {
    }

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
        if (status == null) status = AccountRequestStatus.PENDING;
    }

    public void markApproved(Long reviewerId, Long entityId, String note) {
        this.status = AccountRequestStatus.APPROVED;
        this.reviewedByUserId = reviewerId;
        this.provisionedEntityId = entityId;
        this.reviewNote = note;
        this.reviewedAt = Instant.now();
    }

    public void markRejected(Long reviewerId, String note) {
        this.status = AccountRequestStatus.REJECTED;
        this.reviewedByUserId = reviewerId;
        this.reviewNote = note;
        this.reviewedAt = Instant.now();
    }

    public Long getId() { return id; }
    public UserRole getRequestedRole() { return requestedRole; }
    public void setRequestedRole(UserRole requestedRole) { this.requestedRole = requestedRole; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getEmployeeCode() { return employeeCode; }
    public void setEmployeeCode(String employeeCode) { this.employeeCode = employeeCode; }
    public LocalDate getHireDate() { return hireDate; }
    public void setHireDate(LocalDate hireDate) { this.hireDate = hireDate; }
    public String getAlternatePhone() { return alternatePhone; }
    public void setAlternatePhone(String alternatePhone) { this.alternatePhone = alternatePhone; }
    public String getAdmissionNumber() { return admissionNumber; }
    public void setAdmissionNumber(String admissionNumber) { this.admissionNumber = admissionNumber; }
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public LocalDate getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(LocalDate dateOfBirth) { this.dateOfBirth = dateOfBirth; }
    public String getStudentAdmissionNumber() { return studentAdmissionNumber; }
    public void setStudentAdmissionNumber(String studentAdmissionNumber) { this.studentAdmissionNumber = studentAdmissionNumber; }
    public String getRelationship() { return relationship; }
    public void setRelationship(String relationship) { this.relationship = relationship; }
    public AccountRequestStatus getStatus() { return status; }
    public String getReviewNote() { return reviewNote; }
    public Long getReviewedByUserId() { return reviewedByUserId; }
    public Instant getReviewedAt() { return reviewedAt; }
    public Long getProvisionedEntityId() { return provisionedEntityId; }
    public Instant getCreatedAt() { return createdAt; }
}
