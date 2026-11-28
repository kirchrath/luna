-- Appointments created before life areas existed belong to the work area.
ALTER TABLE appointment ADD COLUMN area TEXT NOT NULL DEFAULT 'WORK';
ALTER TABLE appointment ALTER COLUMN area DROP DEFAULT;
