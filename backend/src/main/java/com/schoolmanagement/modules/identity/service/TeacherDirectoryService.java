package com.schoolmanagement.modules.identity.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.schoolmanagement.modules.identity.dto.TeacherProfileResponse;
import com.schoolmanagement.modules.identity.entity.AppUser;
import com.schoolmanagement.modules.identity.entity.TeacherProfile;
import com.schoolmanagement.modules.identity.repository.AppUserRepository;
import com.schoolmanagement.modules.identity.repository.TeacherProfileRepository;

@Service
public class TeacherDirectoryService {

    private final TeacherProfileRepository teacherProfileRepository;
    private final AppUserRepository appUserRepository;

    public TeacherDirectoryService(
            TeacherProfileRepository teacherProfileRepository,
            AppUserRepository appUserRepository) {
        this.teacherProfileRepository = teacherProfileRepository;
        this.appUserRepository = appUserRepository;
    }

    @Transactional(readOnly = true)
    public List<TeacherProfileResponse> getActiveTeachers() {
        return teacherProfileRepository.findByActiveTrueOrderByEmployeeCodeAsc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private TeacherProfileResponse toResponse(TeacherProfile teacher) {
        AppUser user = appUserRepository.findById(teacher.getUserId()).orElse(null);
        return new TeacherProfileResponse(
                teacher.getId(),
                teacher.getUserId(),
                user == null ? null : user.getFullName(),
                user == null ? null : user.getEmail(),
                user == null ? null : user.getPhone(),
                teacher.getEmployeeCode(),
                teacher.getHireDate(),
                teacher.isActive()
        );
    }
}
