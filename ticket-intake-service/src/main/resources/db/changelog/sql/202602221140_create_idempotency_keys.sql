--liquibase formatted sql

--changeset liquibase:202602221140-1
CREATE TABLE IF NOT EXISTS idempotency_keys (
    id BIGSERIAL PRIMARY KEY,
    endpoint VARCHAR(128) NOT NULL,
    idempotency_key VARCHAR(255) NOT NULL,
    request_hash VARCHAR(64) NOT NULL,
    response_hash VARCHAR(64) NOT NULL,
    response_payload TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NOT NULL
);

--changeset liquibase:202602221140-2
CREATE UNIQUE INDEX IF NOT EXISTS uk_idempotency_keys_endpoint_key
    ON idempotency_keys (endpoint, idempotency_key);

--changeset liquibase:202602221140-3
CREATE INDEX IF NOT EXISTS idx_idempotency_keys_expires_at
    ON idempotency_keys (expires_at);
