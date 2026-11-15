package com.schoolmanagement.modules.identity.controller;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.schoolmanagement.modules.identity.dto.TeacherProfileResponse;
import com.schoolmanagement.modules.identity.service.TeacherDirectoryService;

@RestController
@RequestMapping("/api/v1/identity/teachers")
@PreAuthorize("hasRole('ADMIN')")
public class TeacherDirectoryController {

    private final TeacherDirectoryService teacherDirectoryService;

    public TeacherDirectoryController(
            TeacherDirectoryService teacherDirectoryService
    ) {
        this.teacherDirectoryService = teacherDirectoryService;
    }

    @GetMapping
    public List<TeacherProfileResponse> getActiveTeachers() {
        return teacherDirectoryService.getActiveTeachers();
    }
}