--liquibase formatted sql
-- Идемпотентные изменения: не падают, если колонки уже есть (повторный запуск / уже применённые миграции).

--changeset liquibase:202621022207-1 splitStatements:false
DO $$
BEGIN
  IF EXISTS (
    SELECT 1
    FROM information_schema.columns
    WHERE table_schema = 'public'
      AND table_name = 'enriched_tickets'
      AND column_name = 'detected_language'
  ) AND NOT EXISTS (
    SELECT 1
    FROM information_schema.columns
    WHERE table_schema = 'public'
      AND table_name = 'enriched_tickets'
      AND column_name = 'language_'
  ) THEN
    ALTER TABLE enriched_tickets RENAME COLUMN detected_language TO language_;
  END IF;
END $$;

--changeset liquibase:202621022207-2 splitStatements:false
DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.columns
    WHERE table_schema = 'public'
      AND table_name = 'enriched_tickets'
      AND column_name = 'type_'
  ) THEN
    ALTER TABLE enriched_tickets ADD COLUMN type_ varchar(255);
  END IF;
END $$;

--changeset liquibase:202621022207-3 splitStatements:false
DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.columns
    WHERE table_schema = 'public'
      AND table_name = 'enriched_tickets'
      AND column_name = 'priority'
  ) THEN
    ALTER TABLE enriched_tickets ADD COLUMN priority int;
  END IF;
END $$;
