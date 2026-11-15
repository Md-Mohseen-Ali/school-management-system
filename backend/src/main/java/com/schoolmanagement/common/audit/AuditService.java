
package com.schoolmanagement.common.audit;

import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import com.schoolmanagement.modules.identity.entity.AppUser;
import com.schoolmanagement.modules.identity.repository.AppUserRepository;

@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;
    private final AppUserRepository appUserRepository;
    private final JsonMapper jsonMapper;

    public AuditService(
            AuditLogRepository auditLogRepository,
            AppUserRepository appUserRepository,
            JsonMapper jsonMapper
    ) {
        this.auditLogRepository = auditLogRepository;
        this.appUserRepository = appUserRepository;
        this.jsonMapper = jsonMapper;
    }

    @Transactional
    public void record(
            String action,
            String entityType,
            String entityId,
            Object oldValues,
            Object newValues
    ) {

        Long actorUserId = resolveActorUserId();

        HttpServletRequest request = resolveCurrentRequest();

        String requestId = request == null
                ? UUID.randomUUID().toString()
                : request.getHeader("X-Request-ID");

        if (requestId == null || requestId.isBlank()) {
            requestId = UUID.randomUUID().toString();
        }

        String ipAddress = request == null
                ? null
                : request.getRemoteAddr();

        JsonNode oldJson = convertToJson(oldValues);
        JsonNode newJson = convertToJson(newValues);

        AuditLog auditLog = new AuditLog(
                actorUserId,
                action,
                entityType,
                entityId,
                oldJson,
                newJson,
                requestId,
                ipAddress
        );

        auditLogRepository.save(auditLog);
    }

    @Transactional(readOnly = true)
    public Page<AuditLogResponse> getAuditLogs(Pageable pageable) {

        return auditLogRepository.findAll(pageable)
                .map(this::toResponse);
    }

    private Long resolveActorUserId() {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            return null;
        }

        return appUserRepository
                .findByEmailIgnoreCase(authentication.getName())
                .map(AppUser::getId)
                .orElse(null);
    }

    private HttpServletRequest resolveCurrentRequest() {

        if (RequestContextHolder.getRequestAttributes()
                instanceof ServletRequestAttributes attributes) {

            return attributes.getRequest();
        }

        return null;
    }

    private JsonNode convertToJson(Object value) {

        if (value == null) {
            return null;
        }

        return jsonMapper.valueToTree(value);
    }

    private AuditLogResponse toResponse(AuditLog log) {

        return new AuditLogResponse(
                log.getId(),
                log.getActorUserId(),
                log.getAction(),
                log.getEntityType(),
                log.getEntityId(),
                log.getOldValues(),
                log.getNewValues(),
                log.getRequestId(),
                log.getIpAddress(),
                log.getOccurredAt()
        );
    }
}
