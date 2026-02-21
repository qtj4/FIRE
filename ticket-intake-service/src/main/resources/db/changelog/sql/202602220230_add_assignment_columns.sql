alter table enriched_tickets
    add column if not exists assigned_manager_id bigint;

alter table enriched_tickets
    add column if not exists assigned_manager_name varchar(255);

alter table enriched_tickets
    add column if not exists assigned_office_id bigint;

alter table enriched_tickets
    add column if not exists assigned_office_name varchar(255);

alter table enriched_tickets
    add column if not exists assignment_status varchar(64);

alter table enriched_tickets
    add column if not exists assignment_message text;

alter table enriched_tickets
    add column if not exists assigned_at timestamp;
