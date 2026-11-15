package com.schoolmanagement.modules.system.controller;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/system")
public class SystemStatusController {

    @GetMapping("/status")
    public Map<String, String> getStatus() {

        return Map.of(
                "application", "school-management-backend",
                "status", "UP",
                "message", "School Management API is running"
        );
    }
}
