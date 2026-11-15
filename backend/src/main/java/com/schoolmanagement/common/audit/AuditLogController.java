
package com.schoolmanagement.common.audit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/audit-logs")
@PreAuthorize("hasRole('ADMIN')")
public class AuditLogController {

    private final AuditService auditService;

    public AuditLogController(AuditService auditService) {
        this.auditService = auditService;
    }

    @GetMapping
    public Page<AuditLogResponse> getAuditLogs(
            @PageableDefault(
                size = 20,
                sort = "occurredAt",
                direction = Sort.Direction.DESC
            )
            Pageable pageable
    ) {
        return auditService.getAuditLogs(pageable);
    }
}
