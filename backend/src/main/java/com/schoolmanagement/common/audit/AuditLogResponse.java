
package com.schoolmanagement.common.audit;

import java.time.Instant;

import tools.jackson.databind.JsonNode;

public record AuditLogResponse(

        Long id,
        Long actorUserId,

        String action,
        String entityType,
        String entityId,

        JsonNode oldValues,
        JsonNode newValues,

        String requestId,
        String ipAddress,

        Instant occurredAt

) {
}
