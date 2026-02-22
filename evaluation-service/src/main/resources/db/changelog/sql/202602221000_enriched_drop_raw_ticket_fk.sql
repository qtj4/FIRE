-- liquibase formatted sql
-- evaluation-service: work only with enriched data, no raw_ticket_id

-- changeset evaluation-service:20260222-1
ALTER TABLE enriched_tickets DROP CONSTRAINT IF EXISTS fk_enriched_raw_ticket;
ALTER TABLE enriched_tickets ALTER COLUMN raw_ticket_id DROP NOT NULL;
