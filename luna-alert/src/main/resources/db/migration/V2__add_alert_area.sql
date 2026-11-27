-- Alerts created before life areas existed belong to the work area.
ALTER TABLE alert ADD COLUMN area TEXT NOT NULL DEFAULT 'WORK';
ALTER TABLE alert ALTER COLUMN area DROP DEFAULT;
