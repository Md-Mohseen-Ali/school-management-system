package com.schoolmanagement.modules.fees.entity;

import java.math.BigDecimal;

import jakarta.persistence.*;

@Entity
@Table(name = "fee_invoice_items")
public class FeeInvoiceItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "invoice_id", nullable = false)
    private FeeInvoice invoice;

    @Column(name = "description", nullable = false, length = 150)
    private String description;

    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    protected FeeInvoiceItem() {
    }

    public FeeInvoiceItem(
            FeeInvoice invoice,
            String description,
            BigDecimal amount
    ) {
        this.invoice = invoice;
        this.description = description;
        this.amount = amount;
    }

    public Long getId() {
        return id;
    }

    public FeeInvoice getInvoice() {
        return invoice;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getAmount() {
        return amount;
    }
}