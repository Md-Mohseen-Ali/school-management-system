package com.schoolmanagement.modules.fees.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.schoolmanagement.modules.fees.entity.FeeInvoiceItem;

public interface FeeInvoiceItemRepository
        extends JpaRepository<FeeInvoiceItem, Long> {

    List<FeeInvoiceItem> findByInvoice_IdOrderByIdAsc(Long invoiceId);
}