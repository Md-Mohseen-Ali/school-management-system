package com.schoolmanagement.modules.fees.entity;

import java.time.Instant;
import java.time.LocalDate;

import jakarta.persistence.*;

import com.schoolmanagement.modules.academic.entity.Student;

@Entity
@Table(
    name = "fee_invoices",
    uniqueConstraints = @UniqueConstraint(
        name = "fee_invoices_invoice_number_key",
        columnNames = "invoice_number"
    )
)
public class FeeInvoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "invoice_number", nullable = false, unique = true, length = 40)
    private String invoiceNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Column(name = "academic_year", nullable = false, length = 9)
    private String academicYear;

    @Column(name = "issue_date", nullable = false)
    private LocalDate issueDate;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private FeeInvoiceStatus status = FeeInvoiceStatus.DRAFT;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected FeeInvoice() {
    }

    public FeeInvoice(
            String invoiceNumber,
            Student student,
            String academicYear,
            LocalDate issueDate,
            LocalDate dueDate,
            Long createdBy
    ) {
        this.invoiceNumber = invoiceNumber;
        this.student = student;
        this.academicYear = academicYear;
        this.issueDate = issueDate;
        this.dueDate = dueDate;
        this.createdBy = createdBy;
        this.status = FeeInvoiceStatus.DRAFT;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getInvoiceNumber() {
        return invoiceNumber;
    }

    public Student getStudent() {
        return student;
    }

    public String getAcademicYear() {
        return academicYear;
    }

    public LocalDate getIssueDate() {
        return issueDate;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public FeeInvoiceStatus getStatus() {
        return status;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void issue() {
        this.status = FeeInvoiceStatus.ISSUED;
    }

    public void cancel() {
        this.status = FeeInvoiceStatus.CANCELLED;
    }
}