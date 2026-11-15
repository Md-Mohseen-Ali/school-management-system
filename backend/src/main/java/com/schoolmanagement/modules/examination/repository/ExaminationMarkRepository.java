package com.schoolmanagement.modules.examination.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.schoolmanagement.modules.examination.entity.ExaminationMark;

public interface ExaminationMarkRepository
        extends JpaRepository<ExaminationMark, Long> {

    Optional<ExaminationMark>
    findByExaminationSubject_IdAndStudent_Id(
            Long examinationSubjectId,
            Long studentId
    );

    List<ExaminationMark>
    findByExaminationSubject_IdOrderByStudent_AdmissionNumberAsc(
            Long examinationSubjectId
    );

    List<ExaminationMark>
    findByExaminationSubject_Examination_IdAndStudent_Id(
            Long examinationId,
            Long studentId
    );

    List<ExaminationMark>
    findByExaminationSubject_Examination_Id(Long examinationId);

    long countByExaminationSubject_Id(Long examinationSubjectId);
}