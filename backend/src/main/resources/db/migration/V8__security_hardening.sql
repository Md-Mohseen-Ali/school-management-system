-- Disable the fixed-password assessment administrator. It is re-enabled only
-- when the deployment operator supplies DEMO_ADMIN_PASSWORD at startup.
UPDATE app_users
SET enabled = FALSE
WHERE LOWER(email) = 'admin@school.demo';

-- Record an explicit administrator attestation before provisioning public requests.
ALTER TABLE account_access_requests
    ADD COLUMN identity_verified BOOLEAN NOT NULL DEFAULT FALSE;

-- Store only one-way hashes of rate-limit keys, never raw email addresses or IPs.
CREATE TABLE account_request_rate_limits (
    rate_key VARCHAR(64) PRIMARY KEY,
    window_started_at TIMESTAMPTZ NOT NULL,
    request_count INTEGER NOT NULL CHECK (request_count > 0)
);

CREATE INDEX idx_account_request_rate_limits_window
    ON account_request_rate_limits (window_started_at);
