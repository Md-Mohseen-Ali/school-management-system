package com.schoolmanagement.modules.examination.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.criteria.Predicate;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.schoolmanagement.modules.academic.entity.Section;
import com.schoolmanagement.modules.academic.entity.Subject;
import com.schoolmanagement.modules.academic.repository.SectionRepository;
import com.schoolmanagement.modules.academic.repository.SubjectRepository;

import com.schoolmanagement.modules.examination.dto.ExaminationRequest;
import com.schoolmanagement.modules.examination.dto.ExaminationResponse;
import com.schoolmanagement.modules.examination.dto.ExaminationSubjectRequest;
import com.schoolmanagement.modules.examination.dto.ExaminationSubjectResponse;
import com.schoolmanagement.modules.examination.entity.Examination;
import com.schoolmanagement.modules.examination.entity.ExaminationStatus;
import com.schoolmanagement.modules.examination.entity.ExaminationSubject;
import com.schoolmanagement.modules.examination.repository.ExaminationRepository;
import com.schoolmanagement.modules.examination.repository.ExaminationSubjectRepository;

@Service
public class ExaminationService {

    private final ExaminationRepository examinationRepository;
    private final ExaminationSubjectRepository examinationSubjectRepository;
    private final SectionRepository sectionRepository;
    private final SubjectRepository subjectRepository;

    public ExaminationService(
            ExaminationRepository examinationRepository,
            ExaminationSubjectRepository examinationSubjectRepository,
            SectionRepository sectionRepository,
            SubjectRepository subjectRepository
    ) {
        this.examinationRepository = examinationRepository;
        this.examinationSubjectRepository = examinationSubjectRepository;
        this.sectionRepository = sectionRepository;
        this.subjectRepository = subjectRepository;
    }

    // CREATE EXAMINATION

    @Transactional
    public ExaminationResponse createExamination(
            ExaminationRequest request
    ) {

        Section section = sectionRepository.findById(request.sectionId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Section not found"
                ));

        String examName = request.examName().trim();
        String academicYear = request.academicYear().trim();

        if (!academicYear.equals(section.getAcademicYear())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Academic year must match the section academic year"
            );
        }

        validateExamDates(request.startDate(), request.endDate());

        if (examinationRepository
                .existsBySection_IdAndExamNameAndAcademicYear(
                        section.getId(),
                        examName,
                        academicYear
                )) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "An examination with this name already exists for this section and academic year"
            );
        }

        Examination examination = new Examination(
                section,
                examName,
                academicYear,
                request.startDate(),
                request.endDate()
        );

        try {
            return toExaminationResponse(
                    examinationRepository.saveAndFlush(examination)
            );
        } catch (DataIntegrityViolationException exception) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Duplicate examination configuration",
                    exception
            );
        }
    }

    // LIST EXAMINATIONS

    @Transactional(readOnly = true)
    public Page<ExaminationResponse> getAllExaminations(
            Long sectionId,
            String academicYear,
            ExaminationStatus status,
            Pageable pageable
    ) {

        Specification<Examination> specification =
                (root, query, cb) -> {

            List<Predicate> predicates = new ArrayList<>();

            if (sectionId != null) {
                predicates.add(
                        cb.equal(root.get("section").get("id"), sectionId)
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

        return examinationRepository.findAll(specification, pageable)
                .map(this::toExaminationResponse);
    }

    // GET EXAMINATION

    @Transactional(readOnly = true)
    public ExaminationResponse getExamination(Long id) {

        return toExaminationResponse(findExamination(id));
    }

    // ADD SUBJECT TO EXAMINATION

    @Transactional
    public ExaminationSubjectResponse addExaminationSubject(
            Long examinationId,
            ExaminationSubjectRequest request
    ) {

        Examination examination = findExamination(examinationId);

        ensureDraft(examination);

        Subject subject = subjectRepository.findById(request.subjectId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Subject not found"
                ));

        if (!subject.isActive()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Cannot add an inactive subject to an examination"
            );
        }

        if (examinationSubjectRepository.existsByExamination_IdAndSubject_Id(
                examinationId,
                subject.getId()
        )) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Subject is already configured for this examination"
            );
        }

        if (request.examinationDate().isBefore(examination.getStartDate())
                || request.examinationDate().isAfter(examination.getEndDate())) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Subject examination date must be within examination start and end dates"
            );
        }

        if (request.maxMarks().signum() <= 0
                || request.passMarks().signum() < 0
                || request.passMarks().compareTo(request.maxMarks()) > 0) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid maximum marks or passing marks"
            );
        }

        ExaminationSubject examinationSubject = new ExaminationSubject(
                examination,
                subject,
                request.examinationDate(),
                request.maxMarks(),
                request.passMarks()
        );

        try {
            return toSubjectResponse(
                    examinationSubjectRepository.saveAndFlush(
                            examinationSubject
                    )
            );
        } catch (DataIntegrityViolationException exception) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Subject is already configured for this examination",
                    exception
            );
        }
    }

    // LIST SUBJECTS OF AN EXAMINATION

    @Transactional(readOnly = true)
    public List<ExaminationSubjectResponse> getExaminationSubjects(
            Long examinationId
    ) {

        findExamination(examinationId);

        return examinationSubjectRepository
                .findByExamination_IdOrderByExaminationDateAsc(examinationId)
                .stream()
                .map(this::toSubjectResponse)
                .toList();
    }

    // SCHEDULE EXAMINATION

    @Transactional
    public ExaminationResponse scheduleExamination(Long id) {

        Examination examination = findExamination(id);

        ensureDraft(examination);

        List<ExaminationSubject> subjects =
                examinationSubjectRepository
                        .findByExamination_IdOrderByExaminationDateAsc(id);

        if (subjects.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Configure at least one subject before scheduling the examination"
            );
        }

        examination.setStatus(ExaminationStatus.SCHEDULED);

        return toExaminationResponse(
                examinationRepository.save(examination)
        );
    }

    // PRIVATE HELPERS

    private Examination findExamination(Long id) {

        return examinationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Examination not found"
                ));
    }

    private void ensureDraft(Examination examination) {

        if (examination.getStatus() != ExaminationStatus.DRAFT) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Only draft examinations can be modified"
            );
        }
    }

    private void validateExamDates(
            LocalDate startDate,
            LocalDate endDate
    ) {

        if (endDate.isBefore(startDate)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Examination end date cannot be before start date"
            );
        }
    }

    private ExaminationResponse toExaminationResponse(
            Examination examination
    ) {

        Section section = examination.getSection();

        return new ExaminationResponse(
                examination.getId(),

                section.getId(),
                section.getSectionName(),
                section.getSchoolClass().getName(),

                examination.getExamName(),
                examination.getAcademicYear(),

                examination.getStartDate(),
                examination.getEndDate(),

                examination.getStatus(),
                examination.getCreatedAt()
        );
    }

    private ExaminationSubjectResponse toSubjectResponse(
            ExaminationSubject examinationSubject
    ) {

        Subject subject = examinationSubject.getSubject();

        return new ExaminationSubjectResponse(
                examinationSubject.getId(),
                examinationSubject.getExamination().getId(),

                subject.getId(),
                subject.getSubjectCode(),
                subject.getName(),

                examinationSubject.getExaminationDate(),

                examinationSubject.getMaxMarks(),
                examinationSubject.getPassMarks()
        );
    }
}