CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE IF NOT EXISTS managers (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    location VARCHAR(100) NOT NULL,
    skills TEXT,
    email VARCHAR(255),
    phone VARCHAR(50),
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_managers_location ON managers(location);
CREATE INDEX IF NOT EXISTS idx_managers_is_active ON managers(is_active);
CREATE INDEX IF NOT EXISTS idx_managers_skills ON managers USING gin(to_tsvector('english', skills));

CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_managers_updated_at 
    BEFORE UPDATE ON managers 
    FOR EACH ROW 
    EXECUTE FUNCTION update_updated_at_column();

INSERT INTO managers (name, location, skills, email, phone, is_active) VALUES
('Алихан Каримов', 'Almaty', 'TECHNICAL,BILLING', 'alikhan.karimov@fire.kz', '+7 701 123 4567', true),
('Динара Асылбекова', 'Almaty', 'CONSULTATION,REQUEST', 'dinara.asylbekova@fire.kz', '+7 701 234 5678', true),
('Ерлан Беков', 'Almaty', 'TECHNICAL,COMPLAINT', 'erlan.bekov@fire.kz', '+7 701 345 6789', true),
('Айгуль Сулейманова', 'Almaty', 'BILLING,CONSULTATION', 'aigul.suleimanova@fire.kz', '+7 701 456 7890', true),
('Бауржан Нуртазин', 'Astana', 'TECHNICAL,REQUEST', 'baurzhan.nurtazin@fire.kz', '+7 702 123 4567', true),
('Мадина Есенова', 'Astana', 'CONSULTATION,COMPLAINT', 'madina.esenova@fire.kz', '+7 702 234 5678', true),
('Тимур Оспанов', 'Shymkent', 'BILLING,TECHNICAL', 'timur.ospanov@fire.kz', '+7 703 123 4567', true),
('Гульнара Садыкова', 'Shymkent', 'REQUEST,CONSULTATION', 'gulnara.sadykova@fire.kz', '+7 703 234 5678', true)
ON CONFLICT DO NOTHING;

CREATE OR REPLACE VIEW active_managers AS
SELECT id, name, location, skills, email, phone, is_active, created_at, updated_at
FROM managers
WHERE is_active = true;

CREATE OR REPLACE FUNCTION get_manager_load_stats()
RETURNS TABLE(
    manager_id BIGINT,
    manager_name VARCHAR,
    location VARCHAR,
    active_tickets INTEGER,
    total_capacity INTEGER
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        m.id,
        m.name,
        m.location,
        COALESCE((SELECT COUNT(*) FROM ticket_assignments WHERE manager_id = m.id AND status = 'ACTIVE'), 0) as active_tickets,
        10 as total_capacity
    FROM managers m
    WHERE m.is_active = true
    ORDER BY m.location, m.name;
END;
$$ LANGUAGE plpgsql;

CREATE TABLE IF NOT EXISTS ticket_assignments (
    id BIGSERIAL PRIMARY KEY,
    ticket_id VARCHAR(255) NOT NULL,
    manager_id BIGINT NOT NULL,
    assigned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(50) DEFAULT 'ACTIVE',
    priority VARCHAR(20),
    category VARCHAR(50),
    FOREIGN KEY (manager_id) REFERENCES managers(id)
);

CREATE INDEX IF NOT EXISTS idx_ticket_assignments_manager_id ON ticket_assignments(manager_id);
CREATE INDEX IF NOT EXISTS idx_ticket_assignments_status ON ticket_assignments(status);
CREATE INDEX IF NOT EXISTS idx_ticket_assignments_assigned_at ON ticket_assignments(assigned_at);

COMMIT;
