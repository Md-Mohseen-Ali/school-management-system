package com.schoolmanagement.modules.examination.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.schoolmanagement.modules.examination.entity.ExaminationSubject;

public interface ExaminationSubjectRepository
        extends JpaRepository<ExaminationSubject, Long> {

    boolean existsByExamination_IdAndSubject_Id(
            Long examinationId,
            Long subjectId
    );

    List<ExaminationSubject>
    findByExamination_IdOrderByExaminationDateAsc(Long examinationId);
}