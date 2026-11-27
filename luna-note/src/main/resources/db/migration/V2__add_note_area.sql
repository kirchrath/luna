-- Notes created before life areas existed belong to the work area.
ALTER TABLE note ADD COLUMN area TEXT NOT NULL DEFAULT 'WORK';
ALTER TABLE note ALTER COLUMN area DROP DEFAULT;
