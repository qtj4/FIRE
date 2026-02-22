-- liquibase formatted sql

-- changeset evaluation-service:20260222-1
WITH ranked AS (
    SELECT id,
           ROW_NUMBER() OVER (PARTITION BY client_guid, raw_ticket_id ORDER BY id DESC) AS rn
    FROM enriched_tickets
    WHERE client_guid IS NOT NULL
      AND raw_ticket_id IS NOT NULL
)
DELETE
FROM enriched_tickets e
USING ranked r
WHERE e.id = r.id
  AND r.rn > 1;

-- changeset evaluation-service:20260222-2
CREATE UNIQUE INDEX IF NOT EXISTS uq_enriched_tickets_client_guid_raw_ticket_id
    ON enriched_tickets (client_guid, raw_ticket_id)
    WHERE client_guid IS NOT NULL
      AND raw_ticket_id IS NOT NULL;
