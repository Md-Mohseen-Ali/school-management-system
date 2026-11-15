package com.schoolmanagement.modules.fees.entity;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.persistence.*;

@Entity
@Table(
    name = "fee_payments",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "fee_payments_payment_reference_key",
            columnNames = "payment_reference"
        ),
        @UniqueConstraint(
            name = "fee_payments_idempotency_key_key",
            columnNames = "idempotency_key"
        )
    }
)
public class FeePayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "invoice_id", nullable = false)
    private FeeInvoice invoice;

    @Column(name = "payment_reference", nullable = false, unique = true, length = 60)
    private String paymentReference;

    @Column(name = "idempotency_key", nullable = false, unique = true, length = 100)
    private String idempotencyKey;

    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 20)
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false, length = 20)
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;

    @Column(name = "received_by")
    private Long receivedBy;

    @Column(name = "paid_at")
    private Instant paidAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected FeePayment() {
    }

    public FeePayment(
            FeeInvoice invoice,
            String paymentReference,
            String idempotencyKey,
            BigDecimal amount,
            PaymentMethod paymentMethod,
            Long receivedBy
    ) {
        this.invoice = invoice;
        this.paymentReference = paymentReference;
        this.idempotencyKey = idempotencyKey;
        this.amount = amount;
        this.paymentMethod = paymentMethod;
        this.receivedBy = receivedBy;
        this.paymentStatus = PaymentStatus.SUCCEEDED;
        this.paidAt = Instant.now();
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public FeeInvoice getInvoice() {
        return invoice;
    }

    public String getPaymentReference() {
        return paymentReference;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public PaymentStatus getPaymentStatus() {
        return paymentStatus;
    }

    public Long getReceivedBy() {
        return receivedBy;
    }

    public Instant getPaidAt() {
        return paidAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}