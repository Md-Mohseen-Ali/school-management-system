package com.schoolmanagement.modules.fees.repository;

import java.util.Optional;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.schoolmanagement.modules.fees.entity.FeeInvoice;

public interface FeeInvoiceRepository extends
        JpaRepository<FeeInvoice, Long>,
        JpaSpecificationExecutor<FeeInvoice> {

    boolean existsByInvoiceNumber(String invoiceNumber);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT i
        FROM FeeInvoice i
        WHERE i.id = :id
        """)
    Optional<FeeInvoice> findByIdForUpdate(@Param("id") Long id);
}