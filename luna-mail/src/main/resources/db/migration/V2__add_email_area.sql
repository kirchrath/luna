-- Emails received before life areas existed belong to the work area.
ALTER TABLE email ADD COLUMN area TEXT NOT NULL DEFAULT 'WORK';
ALTER TABLE email ALTER COLUMN area DROP DEFAULT;
