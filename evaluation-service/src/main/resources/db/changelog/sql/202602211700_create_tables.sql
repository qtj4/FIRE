-- liquibase formatted sql

-- changeset evaluation-service:20260221-1
CREATE TABLE IF NOT EXISTS offices (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    address VARCHAR(500) NOT NULL,
    latitude DECIMAL(10, 8),
    longitude DECIMAL(11, 8)
);

-- changeset evaluation-service:20260221-2
CREATE TABLE IF NOT EXISTS managers (
    id BIGSERIAL PRIMARY KEY,
    full_name VARCHAR(255) NOT NULL,
    position VARCHAR(255),
    office_name VARCHAR(255),
    skills VARCHAR(255),
    active_tickets_count INT DEFAULT 0
);

-- changeset evaluation-service:20260221-3
CREATE TABLE IF NOT EXISTS raw_tickets (
    id BIGSERIAL PRIMARY KEY,
    client_guid UUID NOT NULL,
    client_gender VARCHAR(50),
    birth_date TIMESTAMP,
    description TEXT,
    attachments TEXT,
    client_segment VARCHAR(50),
    country VARCHAR(100),
    region VARCHAR(100),
    city VARCHAR(100),
    street VARCHAR(255),
    house_number VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- changeset evaluation-service:20260221-4
CREATE TABLE IF NOT EXISTS enriched_tickets (
    id BIGSERIAL PRIMARY KEY,
    raw_ticket_id BIGINT NOT NULL,
    client_guid UUID,
    type VARCHAR(100),
    priority INT,
    summary TEXT,
    detected_language VARCHAR(10),
    sentiment VARCHAR(50),
    latitude DECIMAL(10, 8),
    longitude DECIMAL(11, 8),
    assigned_office_id BIGINT,
    assigned_manager_id BIGINT,
    enriched_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_enriched_raw_ticket FOREIGN KEY (raw_ticket_id) REFERENCES raw_tickets(id),
    CONSTRAINT fk_enriched_office FOREIGN KEY (assigned_office_id) REFERENCES offices(id),
    CONSTRAINT fk_enriched_manager FOREIGN KEY (assigned_manager_id) REFERENCES managers(id)
);
