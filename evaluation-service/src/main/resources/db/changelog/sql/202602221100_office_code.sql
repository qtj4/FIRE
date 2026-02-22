-- liquibase formatted sql
-- Add office code: offices.code, managers.office_code; match managers to office by code.

-- changeset evaluation-service:20260222-2
ALTER TABLE offices ADD COLUMN IF NOT EXISTS code VARCHAR(50);
UPDATE offices SET code = 'O' || id WHERE code IS NULL;
ALTER TABLE offices ALTER COLUMN code SET NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS idx_offices_code ON offices(code);

-- changeset evaluation-service:20260222-3
ALTER TABLE managers ADD COLUMN IF NOT EXISTS office_code VARCHAR(50);
-- Backfill: привязать менеджеров к коду офиса по имени офиса
UPDATE managers m SET office_code = (SELECT o.code FROM offices o WHERE o.name = m.office_name LIMIT 1)
WHERE m.office_code IS NULL AND m.office_name IS NOT NULL;
