ALTER TABLE inbound_event ADD COLUMN source_id UUID;

CREATE UNIQUE INDEX inbound_event_source_source_id_idx ON inbound_event (source, source_id);
