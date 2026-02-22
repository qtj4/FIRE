-- liquibase formatted sql

-- changeset evaluation-service:20260222-3
CREATE INDEX IF NOT EXISTS idx_raw_tickets_client_guid
    ON raw_tickets (client_guid);

CREATE INDEX IF NOT EXISTS idx_enriched_tickets_raw_ticket_latest
    ON enriched_tickets (raw_ticket_id, enriched_at DESC, id DESC);

CREATE INDEX IF NOT EXISTS idx_enriched_tickets_assigned_manager_id
    ON enriched_tickets (assigned_manager_id);

CREATE INDEX IF NOT EXISTS idx_enriched_tickets_assigned_office_id
    ON enriched_tickets (assigned_office_id);

-- changeset evaluation-service:20260222-4
CREATE OR REPLACE VIEW vw_lead_distribution_result AS
SELECT
    rt.id AS lead_id,
    rt.client_guid,
    rt.created_at AS lead_created_at,
    rt.client_segment,
    rt.country,
    rt.region,
    rt.city,
    rt.street,
    rt.house_number,
    et.id AS ai_analytics_id,
    et.enriched_at AS ai_enriched_at,
    et.type AS ai_type,
    et.priority AS ai_priority,
    et.sentiment AS ai_sentiment,
    et.detected_language AS ai_language,
    et.summary AS ai_summary,
    et.latitude AS ai_latitude,
    et.longitude AS ai_longitude,
    et.assigned_office_id,
    o.name AS assigned_office_name,
    o.address AS assigned_office_address,
    et.assigned_manager_id,
    m.full_name AS assigned_manager_name,
    m.position AS assigned_manager_position,
    m.skills AS assigned_manager_skills,
    CASE
        WHEN et.id IS NULL THEN 'IN_QUEUE'
        WHEN et.assigned_manager_id IS NULL THEN 'UNASSIGNED'
        ELSE 'ASSIGNED'
    END AS distribution_status
FROM raw_tickets rt
LEFT JOIN LATERAL (
    SELECT e.*
    FROM enriched_tickets e
    WHERE e.raw_ticket_id = rt.id
    ORDER BY e.enriched_at DESC NULLS LAST, e.id DESC
    LIMIT 1
) et ON TRUE
LEFT JOIN offices o ON o.id = et.assigned_office_id
LEFT JOIN managers m ON m.id = et.assigned_manager_id;
