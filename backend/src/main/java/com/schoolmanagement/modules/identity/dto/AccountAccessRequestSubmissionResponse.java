package com.schoolmanagement.modules.identity.dto;

import java.time.Instant;
import com.schoolmanagement.modules.identity.entity.AccountRequestStatus;

public record AccountAccessRequestSubmissionResponse(
        Long requestId,
        AccountRequestStatus status,
        Instant submittedAt,
        String message
) {
}
