package com.schoolmanagement.modules.identity.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Database-backed fixed-window counters. Keys are SHA-256 digests so raw
 * email addresses and network addresses are not persisted in this table.
 */
@Service
public class AccountRequestRateLimitService {

    private static final String UPSERT_SQL = """
            INSERT INTO account_request_rate_limits
                (rate_key, window_started_at, request_count)
            VALUES (?, CURRENT_TIMESTAMP, 1)
            ON CONFLICT (rate_key) DO UPDATE
            SET window_started_at = CASE
                    WHEN account_request_rate_limits.window_started_at
                         <= CURRENT_TIMESTAMP - INTERVAL '15 minutes'
                    THEN CURRENT_TIMESTAMP
                    ELSE account_request_rate_limits.window_started_at
                END,
                request_count = CASE
                    WHEN account_request_rate_limits.window_started_at
                         <= CURRENT_TIMESTAMP - INTERVAL '15 minutes'
                    THEN 1
                    ELSE account_request_rate_limits.request_count + 1
                END
            RETURNING request_count
            """;

    private final JdbcTemplate jdbcTemplate;
    private final AtomicInteger cleanupCounter = new AtomicInteger();

    public AccountRequestRateLimitService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Records an attempt in its own transaction so failed/blocked submissions
     * still count even when the surrounding request transaction rolls back.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int recordAttempt(String bucketType, String rawKey) {
        int count = jdbcTemplate.queryForObject(
                UPSERT_SQL,
                Integer.class,
                sha256(bucketType + ":" + rawKey));

        if (cleanupCounter.incrementAndGet() % 100 == 0) {
            jdbcTemplate.update("""
                    DELETE FROM account_request_rate_limits
                    WHERE window_started_at < CURRENT_TIMESTAMP - INTERVAL '1 day'
                    """);
        }

        return count;
    }

    private String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
