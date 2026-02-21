-- liquibase formatted sql
-- Add columns for compatibility with ticket-intake schema (may already exist from evaluation-service)

-- changeset evaluation-service:20260221-5
ALTER TABLE offices ADD COLUMN IF NOT EXISTS latitude DECIMAL(10, 8);
ALTER TABLE offices ADD COLUMN IF NOT EXISTS longitude DECIMAL(11, 8);

-- changeset evaluation-service:20260221-6
ALTER TABLE enriched_tickets ADD COLUMN IF NOT EXISTS type VARCHAR(100);
ALTER TABLE enriched_tickets ADD COLUMN IF NOT EXISTS priority INT;

-- changeset evaluation-service:20260221-7
ALTER TABLE enriched_tickets ADD COLUMN IF NOT EXISTS assigned_manager_id BIGINT;
ALTER TABLE enriched_tickets DROP CONSTRAINT IF EXISTS fk_enriched_manager;
ALTER TABLE enriched_tickets ADD CONSTRAINT fk_enriched_manager FOREIGN KEY (assigned_manager_id) REFERENCES managers(id);
