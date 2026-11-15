package com.schoolmanagement.modules.identity.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.schoolmanagement.modules.identity.dto.TeacherProfileResponse;
import com.schoolmanagement.modules.identity.entity.TeacherProfile;
import com.schoolmanagement.modules.identity.repository.TeacherProfileRepository;

@Service
public class TeacherDirectoryService {

    private final TeacherProfileRepository teacherProfileRepository;

    public TeacherDirectoryService(
            TeacherProfileRepository teacherProfileRepository
    ) {
        this.teacherProfileRepository = teacherProfileRepository;
    }

    @Transactional(readOnly = true)
    public List<TeacherProfileResponse> getActiveTeachers() {

        return teacherProfileRepository
                .findByActiveTrueOrderByEmployeeCodeAsc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private TeacherProfileResponse toResponse(TeacherProfile teacher) {

        return new TeacherProfileResponse(
                teacher.getId(),
                teacher.getUserId(),
                teacher.getEmployeeCode(),
                teacher.getHireDate(),
                teacher.isActive()
        );
    }
}