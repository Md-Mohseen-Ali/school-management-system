
package com.schoolmanagement.modules.fees.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.schoolmanagement.common.audit.AuditService;

import com.schoolmanagement.modules.academic.entity.Student;
import com.schoolmanagement.modules.academic.repository.StudentGuardianRepository;
import com.schoolmanagement.modules.academic.repository.StudentRepository;

import com.schoolmanagement.modules.fees.dto.FeePaymentRequest;
import com.schoolmanagement.modules.fees.dto.FeePaymentResponse;

import com.schoolmanagement.modules.fees.entity.FeeInvoice;
import com.schoolmanagement.modules.fees.entity.FeeInvoiceStatus;
import com.schoolmanagement.modules.fees.entity.FeeInvoiceItem;
import com.schoolmanagement.modules.fees.entity.FeePayment;
import com.schoolmanagement.modules.fees.entity.PaymentMethod;
import com.schoolmanagement.modules.fees.entity.PaymentStatus;

import com.schoolmanagement.modules.fees.repository.FeeInvoiceItemRepository;
import com.schoolmanagement.modules.fees.repository.FeeInvoiceRepository;
import com.schoolmanagement.modules.fees.repository.FeePaymentRepository;

import com.schoolmanagement.modules.identity.entity.AppUser;
import com.schoolmanagement.modules.identity.entity.GuardianProfile;
import com.schoolmanagement.modules.identity.repository.AppUserRepository;
import com.schoolmanagement.modules.identity.repository.GuardianProfileRepository;

@Service
public class FeePaymentService {

    private final FeeInvoiceRepository invoiceRepository;
    private final FeeInvoiceItemRepository itemRepository;
    private final FeePaymentRepository paymentRepository;

    private final GuardianProfileRepository guardianRepository;
    private final StudentGuardianRepository studentGuardianRepository;
    private final StudentRepository studentRepository;
    private final AppUserRepository appUserRepository;

    private final AuditService auditService;

    public FeePaymentService(
            FeeInvoiceRepository invoiceRepository,
            FeeInvoiceItemRepository itemRepository,
            FeePaymentRepository paymentRepository,
            GuardianProfileRepository guardianRepository,
            StudentGuardianRepository studentGuardianRepository,
            StudentRepository studentRepository,
            AppUserRepository appUserRepository,
            AuditService auditService
    ) {
        this.invoiceRepository = invoiceRepository;
        this.itemRepository = itemRepository;
        this.paymentRepository = paymentRepository;
        this.guardianRepository = guardianRepository;
        this.studentGuardianRepository = studentGuardianRepository;
        this.studentRepository = studentRepository;
        this.appUserRepository = appUserRepository;
        this.auditService = auditService;
    }

    @Transactional
    public FeePaymentResponse processPayment(
            Long invoiceId,
            FeePaymentRequest request,
            String authenticatedEmail,
            boolean admin
    ) {

        // Lock invoice row to prevent concurrent overpayment.
        FeeInvoice invoice = invoiceRepository.findByIdForUpdate(invoiceId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Invoice not found"
                ));

        if (!admin) {
            verifyGuardianAccess(invoice, authenticatedEmail);
        }

        // The assessment environment never collects or records real-money
        // payments. This rule applies to every role, including administrators.
        if (request.paymentMethod() != PaymentMethod.SIMULATED) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Only simulated payments are supported"
            );
        }

        String idempotencyKey = request.idempotencyKey().trim();

        // A retry with the same key returns the original payment.
        var existingPayment = paymentRepository
                .findByIdempotencyKey(idempotencyKey);

        if (existingPayment.isPresent()) {

            FeePayment existing = existingPayment.get();

            boolean sameRequest =
                    existing.getInvoice().getId().equals(invoiceId)
                    && existing.getAmount().compareTo(request.amount()) == 0
                    && existing.getPaymentMethod() == request.paymentMethod();

            if (!sameRequest) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Idempotency key has already been used for a different payment request"
                );
            }

            return toResponse(existing);
        }

        if (invoice.getStatus() != FeeInvoiceStatus.ISSUED) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Payments can only be made against issued invoices"
            );
        }

        BigDecimal totalAmount = calculateInvoiceTotal(invoiceId);

        BigDecimal amountPaid = paymentRepository
                .sumSuccessfulPayments(invoiceId);

        BigDecimal balance = totalAmount.subtract(amountPaid);

        if (balance.signum() <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Invoice has already been paid in full"
            );
        }

        if (request.amount().compareTo(balance) > 0) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Payment exceeds outstanding balance. Current balance: "
                            + balance
            );
        }

        Long receivedBy = null;

        if (admin) {

            AppUser actor = appUserRepository
                    .findByEmailIgnoreCase(authenticatedEmail)
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.UNAUTHORIZED,
                            "Authenticated administrator not found"
                    ));

            receivedBy = actor.getId();
        }

        String paymentReference = "PAY-" + UUID.randomUUID();

        FeePayment payment = new FeePayment(
                invoice,
                paymentReference,
                idempotencyKey,
                request.amount(),
                request.paymentMethod(),
                receivedBy
        );

        try {

            FeePayment saved = paymentRepository.saveAndFlush(payment);

            auditService.record(
                    "FEE_PAYMENT_SUCCEEDED",
                    "FEE_PAYMENT",
                    saved.getId().toString(),
                    null,
                    Map.of(
                            "invoiceId", invoiceId,
                            "invoiceNumber", invoice.getInvoiceNumber(),
                            "paymentReference", saved.getPaymentReference(),
                            "amount", saved.getAmount(),
                            "paymentMethod", saved.getPaymentMethod(),
                            "paymentStatus", saved.getPaymentStatus()
                    )
            );

            return toResponse(saved);

        } catch (DataIntegrityViolationException exception) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Duplicate payment reference or idempotency key. Retry using the original payment request.",
                    exception
            );
        }
    }

    @Transactional(readOnly = true)
    public List<FeePaymentResponse> getPaymentHistory(
            Long invoiceId,
            String authenticatedEmail,
            boolean admin
    ) {

        FeeInvoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Invoice not found"
                ));

        if (!admin) {
            verifyGuardianAccess(invoice, authenticatedEmail);
        }

        return paymentRepository
                .findByInvoice_IdOrderByCreatedAtDesc(invoiceId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<FeePaymentResponse> getStudentPaymentHistory(
            Long invoiceId,
            String authenticatedEmail
    ) {
        Student student = studentRepository.findByUser_EmailIgnoreCase(authenticatedEmail)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "No student profile is linked to this account"));

        FeeInvoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Invoice not found"));
        if (invoice.getStatus() != FeeInvoiceStatus.ISSUED
                || !invoice.getStudent().getId().equals(student.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Invoice not found for this student");
        }

        return paymentRepository.findByInvoice_IdOrderByCreatedAtDesc(invoiceId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private BigDecimal calculateInvoiceTotal(Long invoiceId) {

        return itemRepository
                .findByInvoice_IdOrderByIdAsc(invoiceId)
                .stream()
                .map(FeeInvoiceItem::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void verifyGuardianAccess(
            FeeInvoice invoice,
            String authenticatedEmail
    ) {

        if (invoice.getStatus() != FeeInvoiceStatus.ISSUED) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Invoice not found"
            );
        }

        GuardianProfile guardian = guardianRepository
                .findByUserEmail(authenticatedEmail)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Guardian profile not found"
                ));

        boolean linked = studentGuardianRepository
                .existsByStudent_IdAndGuardian_Id(
                        invoice.getStudent().getId(),
                        guardian.getId()
                );

        if (!linked) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Invoice not found for this guardian"
            );
        }
    }

    private FeePaymentResponse toResponse(FeePayment payment) {

        return new FeePaymentResponse(
                payment.getId(),
                payment.getInvoice().getId(),
                payment.getInvoice().getInvoiceNumber(),
                payment.getPaymentReference(),
                payment.getAmount(),
                payment.getPaymentMethod(),
                payment.getPaymentStatus(),
                payment.getPaidAt(),
                payment.getCreatedAt()
        );
    }
}
