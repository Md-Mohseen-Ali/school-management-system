
package com.schoolmanagement.modules.fees.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import jakarta.persistence.criteria.Predicate;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.schoolmanagement.common.audit.AuditService;

import com.schoolmanagement.modules.academic.entity.Student;
import com.schoolmanagement.modules.academic.repository.StudentGuardianRepository;
import com.schoolmanagement.modules.academic.repository.StudentRepository;

import com.schoolmanagement.modules.fees.dto.FeeInvoiceItemRequest;
import com.schoolmanagement.modules.fees.dto.FeeInvoiceItemResponse;
import com.schoolmanagement.modules.fees.dto.FeeInvoiceRequest;
import com.schoolmanagement.modules.fees.dto.FeeInvoiceResponse;

import com.schoolmanagement.modules.fees.entity.FeeInvoice;
import com.schoolmanagement.modules.fees.entity.FeeInvoiceItem;
import com.schoolmanagement.modules.fees.entity.FeeInvoiceStatus;
import com.schoolmanagement.modules.fees.entity.PaymentStatus;

import com.schoolmanagement.modules.fees.repository.FeeInvoiceItemRepository;
import com.schoolmanagement.modules.fees.repository.FeeInvoiceRepository;
import com.schoolmanagement.modules.fees.repository.FeePaymentRepository;

import com.schoolmanagement.modules.identity.entity.AppUser;
import com.schoolmanagement.modules.identity.entity.GuardianProfile;
import com.schoolmanagement.modules.identity.repository.AppUserRepository;
import com.schoolmanagement.modules.identity.repository.GuardianProfileRepository;

@Service
public class FeeInvoiceService {

    private final FeeInvoiceRepository invoiceRepository;
    private final FeeInvoiceItemRepository itemRepository;
    private final FeePaymentRepository paymentRepository;

    private final StudentRepository studentRepository;
    private final StudentGuardianRepository studentGuardianRepository;

    private final AppUserRepository appUserRepository;
    private final GuardianProfileRepository guardianRepository;

    private final AuditService auditService;

    public FeeInvoiceService(
            FeeInvoiceRepository invoiceRepository,
            FeeInvoiceItemRepository itemRepository,
            FeePaymentRepository paymentRepository,
            StudentRepository studentRepository,
            StudentGuardianRepository studentGuardianRepository,
            AppUserRepository appUserRepository,
            GuardianProfileRepository guardianRepository,
            AuditService auditService
    ) {
        this.invoiceRepository = invoiceRepository;
        this.itemRepository = itemRepository;
        this.paymentRepository = paymentRepository;
        this.studentRepository = studentRepository;
        this.studentGuardianRepository = studentGuardianRepository;
        this.appUserRepository = appUserRepository;
        this.guardianRepository = guardianRepository;
        this.auditService = auditService;
    }

    @Transactional
    public FeeInvoiceResponse createInvoice(
            FeeInvoiceRequest request,
            String authenticatedEmail
    ) {

        String invoiceNumber = request.invoiceNumber()
                .trim()
                .toUpperCase();

        String academicYear = request.academicYear().trim();

        if (request.dueDate().isBefore(request.issueDate())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Due date cannot be before issue date"
            );
        }

        if (invoiceRepository.existsByInvoiceNumber(invoiceNumber)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Invoice number already exists"
            );
        }

        Student student = studentRepository.findById(request.studentId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Student not found"
                ));

        AppUser actor = appUserRepository
                .findByEmailIgnoreCase(authenticatedEmail)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Authenticated user not found"
                ));

        FeeInvoice invoice = new FeeInvoice(
                invoiceNumber,
                student,
                academicYear,
                request.issueDate(),
                request.dueDate(),
                actor.getId()
        );

        try {
            FeeInvoice saved = invoiceRepository.saveAndFlush(invoice);

            auditService.record(
                    "FEE_INVOICE_CREATED",
                    "FEE_INVOICE",
                    saved.getId().toString(),
                    null,
                    Map.of(
                            "invoiceNumber", saved.getInvoiceNumber(),
                            "studentId", student.getId(),
                            "academicYear", saved.getAcademicYear(),
                            "issueDate", saved.getIssueDate(),
                            "dueDate", saved.getDueDate(),
                            "status", saved.getStatus()
                    )
            );

            return toResponse(saved);

        } catch (org.springframework.dao.DataIntegrityViolationException exception) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Invoice number already exists",
                    exception
            );
        }
    }

    @Transactional
    public FeeInvoiceItemResponse addInvoiceItem(
            Long invoiceId,
            FeeInvoiceItemRequest request
    ) {

        FeeInvoice invoice = findInvoiceForUpdate(invoiceId);

        ensureDraft(invoice);

        FeeInvoiceItem item = new FeeInvoiceItem(
                invoice,
                request.description().trim(),
                request.amount()
        );

        FeeInvoiceItem saved = itemRepository.saveAndFlush(item);

        auditService.record(
                "FEE_INVOICE_ITEM_ADDED",
                "FEE_INVOICE",
                invoiceId.toString(),
                null,
                Map.of(
                        "itemId", saved.getId(),
                        "description", saved.getDescription(),
                        "amount", saved.getAmount()
                )
        );

        return toItemResponse(saved);
    }

    @Transactional
    public FeeInvoiceResponse issueInvoice(Long invoiceId) {

        FeeInvoice invoice = findInvoiceForUpdate(invoiceId);

        ensureDraft(invoice);

        BigDecimal total = calculateTotal(invoiceId);

        if (total.signum() <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Cannot issue an invoice without positive-value items"
            );
        }

        invoice.issue();

        FeeInvoice saved = invoiceRepository.save(invoice);

        auditService.record(
                "FEE_INVOICE_ISSUED",
                "FEE_INVOICE",
                invoiceId.toString(),
                Map.of("status", FeeInvoiceStatus.DRAFT),
                Map.of(
                        "status", saved.getStatus(),
                        "totalAmount", total
                )
        );

        return toResponse(saved);
    }

    @Transactional
    public FeeInvoiceResponse cancelInvoice(Long invoiceId) {

        FeeInvoice invoice = findInvoiceForUpdate(invoiceId);

        if (invoice.getStatus() == FeeInvoiceStatus.CANCELLED) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Invoice is already cancelled"
            );
        }

        boolean hasSuccessfulPayment = paymentRepository
                .findByInvoice_IdOrderByCreatedAtDesc(invoiceId)
                .stream()
                .anyMatch(payment ->
                        payment.getPaymentStatus() == PaymentStatus.SUCCEEDED
                );

        if (hasSuccessfulPayment) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "An invoice with successful payments cannot be cancelled"
            );
        }

        FeeInvoiceStatus oldStatus = invoice.getStatus();

        invoice.cancel();

        FeeInvoice saved = invoiceRepository.save(invoice);

        auditService.record(
                "FEE_INVOICE_CANCELLED",
                "FEE_INVOICE",
                invoiceId.toString(),
                Map.of("status", oldStatus),
                Map.of("status", saved.getStatus())
        );

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public Page<FeeInvoiceResponse> getAllInvoices(
            Long studentId,
            String academicYear,
            FeeInvoiceStatus status,
            String authenticatedEmail,
            boolean admin,
            Pageable pageable
    ) {

        List<Long> permittedStudentIds = null;

        if (!admin) {

            GuardianProfile guardian = findGuardian(authenticatedEmail);

            permittedStudentIds = studentGuardianRepository
                    .findByGuardian_Id(guardian.getId())
                    .stream()
                    .map(link -> link.getStudent().getId())
                    .distinct()
                    .toList();

            if (permittedStudentIds.isEmpty()) {
                return Page.empty(pageable);
            }
        }

        final List<Long> guardianStudentIds = permittedStudentIds;

        Specification<FeeInvoice> specification =
                (root, query, cb) -> {

            List<Predicate> predicates = new ArrayList<>();

            if (studentId != null) {
                predicates.add(
                        cb.equal(root.get("student").get("id"), studentId)
                );
            }

            if (guardianStudentIds != null) {

                predicates.add(
                        root.get("student").get("id").in(guardianStudentIds)
                );

                predicates.add(
                        cb.equal(
                                root.get("status"),
                                FeeInvoiceStatus.ISSUED
                        )
                );
            }

            if (academicYear != null && !academicYear.isBlank()) {
                predicates.add(
                        cb.equal(
                                root.get("academicYear"),
                                academicYear.trim()
                        )
                );
            }

            if (status != null) {
                predicates.add(
                        cb.equal(root.get("status"), status)
                );
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return invoiceRepository.findAll(specification, pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public FeeInvoiceResponse getInvoice(
            Long invoiceId,
            String authenticatedEmail,
            boolean admin
    ) {

        FeeInvoice invoice = findInvoice(invoiceId);

        if (!admin) {

            if (invoice.getStatus() != FeeInvoiceStatus.ISSUED) {
                throw new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Invoice not found"
                );
            }

            verifyGuardianAccess(
                    invoice.getStudent().getId(),
                    authenticatedEmail
            );
        }

        return toResponse(invoice);
    }

    private FeeInvoice findInvoice(Long id) {

        return invoiceRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Invoice not found"
                ));
    }

    private FeeInvoice findInvoiceForUpdate(Long id) {

        return invoiceRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Invoice not found"
                ));
    }

    private void ensureDraft(FeeInvoice invoice) {

        if (invoice.getStatus() != FeeInvoiceStatus.DRAFT) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Only draft invoices can be modified"
            );
        }
    }

    private BigDecimal calculateTotal(Long invoiceId) {

        return itemRepository
                .findByInvoice_IdOrderByIdAsc(invoiceId)
                .stream()
                .map(FeeInvoiceItem::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calculatePaidAmount(Long invoiceId) {

        return paymentRepository
                .findByInvoice_IdOrderByCreatedAtDesc(invoiceId)
                .stream()
                .filter(payment ->
                        payment.getPaymentStatus() == PaymentStatus.SUCCEEDED
                )
                .map(payment -> payment.getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private GuardianProfile findGuardian(String email) {

        return guardianRepository.findByUserEmail(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Guardian profile not found"
                ));
    }

    private void verifyGuardianAccess(
            Long studentId,
            String email
    ) {

        GuardianProfile guardian = findGuardian(email);

        boolean linked = studentGuardianRepository
                .existsByStudent_IdAndGuardian_Id(
                        studentId,
                        guardian.getId()
                );

        if (!linked) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Invoice not found for this guardian"
            );
        }
    }

    private FeeInvoiceItemResponse toItemResponse(
            FeeInvoiceItem item
    ) {

        return new FeeInvoiceItemResponse(
                item.getId(),
                item.getDescription(),
                item.getAmount()
        );
    }

    private FeeInvoiceResponse toResponse(FeeInvoice invoice) {

        List<FeeInvoiceItemResponse> items =
                itemRepository
                        .findByInvoice_IdOrderByIdAsc(invoice.getId())
                        .stream()
                        .map(this::toItemResponse)
                        .toList();

        BigDecimal totalAmount = items.stream()
                .map(FeeInvoiceItemResponse::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal amountPaid = calculatePaidAmount(invoice.getId());

        BigDecimal balance = totalAmount.subtract(amountPaid);

        Student student = invoice.getStudent();

        return new FeeInvoiceResponse(
                invoice.getId(),
                invoice.getInvoiceNumber(),

                student.getId(),
                student.getAdmissionNumber(),
                student.getFirstName() + " " + student.getLastName(),

                invoice.getAcademicYear(),
                invoice.getIssueDate(),
                invoice.getDueDate(),

                invoice.getStatus(),

                items,

                totalAmount,
                amountPaid,
                balance,

                invoice.getCreatedAt()
        );
    }
}
