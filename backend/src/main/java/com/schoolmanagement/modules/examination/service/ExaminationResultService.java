package com.schoolmanagement.modules.examination.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Map;

import com.schoolmanagement.common.audit.AuditService;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.schoolmanagement.modules.academic.entity.Section;
import com.schoolmanagement.modules.academic.entity.Student;
import com.schoolmanagement.modules.academic.entity.StudentEnrollment;
import com.schoolmanagement.modules.academic.repository.StudentEnrollmentRepository;
import com.schoolmanagement.modules.academic.repository.StudentRepository;

import com.schoolmanagement.modules.examination.dto.ExaminationReportCardResponse;
import com.schoolmanagement.modules.examination.dto.ExaminationResponse;
import com.schoolmanagement.modules.examination.dto.ExaminationSubjectResultResponse;

import com.schoolmanagement.modules.examination.entity.ExamAttendanceStatus;
import com.schoolmanagement.modules.examination.entity.Examination;
import com.schoolmanagement.modules.examination.entity.ExaminationMark;
import com.schoolmanagement.modules.examination.entity.ExaminationStatus;
import com.schoolmanagement.modules.examination.entity.ExaminationSubject;
import com.schoolmanagement.modules.examination.entity.GradingScale;

import com.schoolmanagement.modules.examination.repository.ExaminationMarkRepository;
import com.schoolmanagement.modules.examination.repository.ExaminationRepository;
import com.schoolmanagement.modules.examination.repository.ExaminationSubjectRepository;
import com.schoolmanagement.modules.examination.repository.GradingScaleRepository;

@Service
public class ExaminationResultService {

    private final ExaminationRepository examinationRepository;
    private final ExaminationSubjectRepository subjectRepository;
    private final ExaminationMarkRepository markRepository;
    private final GradingScaleRepository gradingScaleRepository;
    private final StudentEnrollmentRepository enrollmentRepository;
    private final StudentRepository studentRepository;
    private final AuditService auditService;

    public ExaminationResultService(
            ExaminationRepository examinationRepository,
            ExaminationSubjectRepository subjectRepository,
            ExaminationMarkRepository markRepository,
            GradingScaleRepository gradingScaleRepository,
            StudentEnrollmentRepository enrollmentRepository,
            StudentRepository studentRepository,
            AuditService auditService
    ) {
        this.examinationRepository = examinationRepository;
        this.subjectRepository = subjectRepository;
        this.markRepository = markRepository;
        this.gradingScaleRepository = gradingScaleRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.studentRepository = studentRepository;
        this.auditService = auditService;
    }

    // ==================================================
    // 1. COMPLETE EXAMINATION
    // ==================================================

    @Transactional
    public ExaminationResponse completeExamination(Long examinationId) {

        Examination examination = findExaminationForUpdate(examinationId);

        if (examination.getStatus() != ExaminationStatus.SCHEDULED) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Only scheduled examinations can be completed"
            );
        }

        List<ExaminationSubject> examinationSubjects =
                subjectRepository
                        .findByExamination_IdOrderByExaminationDateAsc(
                                examinationId
                        );

        if (examinationSubjects.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Examination has no configured subjects"
            );
        }

        // Ensure the academic year's grading rules are complete.
        validateGradingScaleCoverage(examination.getAcademicYear());

        Long sectionId = examination.getSection().getId();

        // Check marks for every subject and every enrolled student.
        for (ExaminationSubject examinationSubject : examinationSubjects) {

            LocalDate examinationDate =
                    examinationSubject.getExaminationDate();

            List<StudentEnrollment> enrollments =
                    enrollmentRepository.findEnrollmentsOnDate(
                            sectionId,
                            examinationDate
                    );

            if (enrollments.isEmpty()) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "No students were enrolled for subject: "
                                + examinationSubject.getSubject().getName()
                );
            }

            Set<Long> expectedStudentIds = new HashSet<>();

            for (StudentEnrollment enrollment : enrollments) {
                expectedStudentIds.add(
                        enrollment.getStudent().getId()
                );
            }

            List<ExaminationMark> marks =
                    markRepository
                            .findByExaminationSubject_IdOrderByStudent_AdmissionNumberAsc(
                                    examinationSubject.getId()
                            );

            Set<Long> markedStudentIds = new HashSet<>();

            for (ExaminationMark mark : marks) {
                markedStudentIds.add(mark.getStudent().getId());
            }

            if (!expectedStudentIds.equals(markedStudentIds)) {

                Set<Long> missingStudentIds =
                        new HashSet<>(expectedStudentIds);

                missingStudentIds.removeAll(markedStudentIds);

                Set<Long> unexpectedStudentIds =
                        new HashSet<>(markedStudentIds);

                unexpectedStudentIds.removeAll(expectedStudentIds);

                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Cannot complete examination for subject "
                                + examinationSubject.getSubject().getName()
                                + ". Missing student IDs: "
                                + missingStudentIds
                                + ". Unexpected student IDs: "
                                + unexpectedStudentIds
                );
            }
        }

        examination.setStatus(ExaminationStatus.COMPLETED);
        Examination saved = examinationRepository.save(examination);
        auditService.record("EXAMINATION_COMPLETED", "EXAMINATION", saved.getId().toString(),
                Map.of("status", ExaminationStatus.SCHEDULED.name()),
                Map.of("status", saved.getStatus().name()));
        return toExaminationResponse(saved);
    }

    // ==================================================
    // 2. PUBLISH EXAMINATION
    // ==================================================

    @Transactional
    public ExaminationResponse publishExamination(Long examinationId) {

        Examination examination = findExaminationForUpdate(examinationId);

        if (examination.getStatus() != ExaminationStatus.COMPLETED) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Only completed examinations can be published"
            );
        }

        examination.setStatus(ExaminationStatus.PUBLISHED);
        Examination saved = examinationRepository.save(examination);
        auditService.record("EXAMINATION_PUBLISHED", "EXAMINATION", saved.getId().toString(),
                Map.of("status", ExaminationStatus.COMPLETED.name()),
                Map.of("status", saved.getStatus().name()));
        return toExaminationResponse(saved);
    }

    // ==================================================
    // 3. GENERATE STUDENT REPORT CARD
    // ==================================================

    @Transactional(readOnly = true)
    public ExaminationReportCardResponse getReportCard(
            Long examinationId,
            Long studentId
    ) {

        Examination examination = examinationRepository
                .findById(examinationId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Examination not found"
                ));

        if (examination.getStatus() != ExaminationStatus.PUBLISHED) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Report cards are available only after result publication"
            );
        }

        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Student not found"
                ));

        Section section = examination.getSection();

        List<ExaminationSubject> examinationSubjects =
                subjectRepository
                        .findByExamination_IdOrderByExaminationDateAsc(
                                examinationId
                        );

        List<ExaminationSubjectResultResponse> subjectResults =
                new ArrayList<>();

        BigDecimal totalObtained = BigDecimal.ZERO;
        BigDecimal totalMaximum = BigDecimal.ZERO;

        boolean enrolledInAtLeastOneSubject = false;
        boolean allSubjectsPassed = true;

        // Calculate subject-wise and overall results.
        for (ExaminationSubject examinationSubject : examinationSubjects) {

            boolean enrolled = enrollmentRepository.existsEnrollmentOnDate(
                    studentId,
                    section.getId(),
                    examinationSubject.getExaminationDate()
            );

            // Ignore subjects for dates when the student was not enrolled.
            if (!enrolled) {
                continue;
            }

            enrolledInAtLeastOneSubject = true;

            ExaminationMark mark = markRepository
                    .findByExaminationSubject_IdAndStudent_Id(
                            examinationSubject.getId(),
                            studentId
                    )
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.CONFLICT,
                            "Published examination is missing marks for "
                                    + examinationSubject.getSubject().getName()
                    ));

            boolean subjectPassed =
                    mark.getAttendanceStatus() == ExamAttendanceStatus.PRESENT
                    && mark.getMarksObtained().compareTo(
                            examinationSubject.getPassMarks()
                    ) >= 0;

            if (!subjectPassed) {
                allSubjectsPassed = false;
            }

            totalObtained = totalObtained.add(
                    mark.getMarksObtained()
            );

            totalMaximum = totalMaximum.add(
                    examinationSubject.getMaxMarks()
            );

            subjectResults.add(
                    toSubjectResult(
                            examinationSubject,
                            mark,
                            subjectPassed
                    )
            );
        }

        if (!enrolledInAtLeastOneSubject || totalMaximum.signum() <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Student was not enrolled for this examination"
            );
        }

        // Overall percentage.
        BigDecimal percentage = totalObtained
                .multiply(BigDecimal.valueOf(100))
                .divide(
                        totalMaximum,
                        2,
                        RoundingMode.HALF_UP
                );

        // Find the grade configured for this academic year.
        GradingScale grade = findGrade(
                examination.getAcademicYear(),
                percentage
        );

        return new ExaminationReportCardResponse(

                examination.getId(),
                examination.getExamName(),
                examination.getAcademicYear(),

                student.getId(),
                student.getAdmissionNumber(),
                student.getFirstName() + " " + student.getLastName(),

                section.getId(),
                section.getSectionName(),
                section.getSchoolClass().getName(),

                subjectResults,

                totalObtained,
                totalMaximum,
                percentage,

                grade.getGrade(),
                grade.getRemarks(),

                allSubjectsPassed
        );
    }

    // ==================================================
    // 4. VALIDATE GRADING SCALE COVERAGE
    // ==================================================

    private void validateGradingScaleCoverage(String academicYear) {

        List<GradingScale> grades =
                new ArrayList<>(
                        gradingScaleRepository
                                .findByAcademicYearOrderByMinPercentageDesc(
                                        academicYear
                                )
                );

        if (grades.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "No grading scales configured for academic year "
                            + academicYear
            );
        }

        // Sort from lowest percentage to highest.
        grades.sort(
                Comparator.comparing(GradingScale::getMinPercentage)
        );

        BigDecimal expectedStart = BigDecimal.ZERO;
        BigDecimal expectedEnd = BigDecimal.valueOf(100);
        BigDecimal increment = new BigDecimal("0.01");

        if (grades.get(0).getMinPercentage().compareTo(expectedStart) != 0) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Grading scale must begin at 0.00 percent"
            );
        }

        if (grades.get(grades.size() - 1)
                .getMaxPercentage().compareTo(expectedEnd) != 0) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Grading scale must cover up to 100.00 percent"
            );
        }

        // Check continuity and prevent overlapping/gapped ranges.
        for (int i = 1; i < grades.size(); i++) {

            BigDecimal previousMaximum =
                    grades.get(i - 1).getMaxPercentage();

            BigDecimal currentMinimum =
                    grades.get(i).getMinPercentage();

            BigDecimal expectedMinimum =
                    previousMaximum.add(increment);

            if (currentMinimum.compareTo(expectedMinimum) != 0) {

                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Grading scales contain an overlapping or missing percentage range"
                );
            }
        }
    }

    // ==================================================
    // 5. FIND GRADE FOR PERCENTAGE
    // ==================================================

    private GradingScale findGrade(
            String academicYear,
            BigDecimal percentage
    ) {

        return gradingScaleRepository
                .findByAcademicYearOrderByMinPercentageDesc(
                        academicYear
                )
                .stream()
                .filter(scale ->
                        percentage.compareTo(
                                scale.getMinPercentage()
                        ) >= 0
                        &&
                        percentage.compareTo(
                                scale.getMaxPercentage()
                        ) <= 0
                )
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "No grading rule found for percentage "
                                + percentage
                ));
    }

    // ==================================================
    // 6. CONVERT SUBJECT RESULT
    // ==================================================

    private ExaminationSubjectResultResponse toSubjectResult(
            ExaminationSubject subject,
            ExaminationMark mark,
            boolean passed
    ) {

        return new ExaminationSubjectResultResponse(

                subject.getId(),

                subject.getSubject().getId(),
                subject.getSubject().getSubjectCode(),
                subject.getSubject().getName(),

                subject.getExaminationDate(),

                mark.getMarksObtained(),
                subject.getMaxMarks(),
                subject.getPassMarks(),

                mark.getAttendanceStatus(),
                passed
        );
    }

    // ==================================================
    // 7. FIND EXAMINATION WITH WRITE LOCK
    // ==================================================

    private Examination findExaminationForUpdate(Long id) {

        return examinationRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Examination not found"
                ));
    }

    // ==================================================
    // 8. CONVERT EXAMINATION RESPONSE
    // ==================================================

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
}