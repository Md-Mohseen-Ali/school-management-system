package com.schoolmanagement.modules.fees.repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.schoolmanagement.modules.fees.entity.FeePayment;

public interface FeePaymentRepository
        extends JpaRepository<FeePayment, Long> {

    Optional<FeePayment> findByIdempotencyKey(String idempotencyKey);

    List<FeePayment> findByInvoice_IdOrderByCreatedAtDesc(
            Long invoiceId
    );

    @Query("""
        SELECT COALESCE(SUM(p.amount), 0)
        FROM FeePayment p
        WHERE p.invoice.id = :invoiceId
          AND p.paymentStatus =
              com.schoolmanagement.modules.fees.entity.PaymentStatus.SUCCEEDED
        """)
    BigDecimal sumSuccessfulPayments(
            @Param("invoiceId") Long invoiceId
    );
}