alter table enriched_tickets
    rename column detected_language to language_;

alter table enriched_tickets
    add column type_ varchar(255);

alter table enriched_tickets
    add column priority int;