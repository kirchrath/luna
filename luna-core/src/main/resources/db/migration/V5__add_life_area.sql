-- Inputs and elements created before life areas existed belong to the work area.
ALTER TABLE inbound_event ADD COLUMN area TEXT NOT NULL DEFAULT 'WORK';
ALTER TABLE inbound_event ALTER COLUMN area DROP DEFAULT;
ALTER TABLE outbound_event ADD COLUMN area TEXT NOT NULL DEFAULT 'WORK';
ALTER TABLE outbound_event ALTER COLUMN area DROP DEFAULT;
