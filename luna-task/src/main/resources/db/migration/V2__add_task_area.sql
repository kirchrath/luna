-- Tasks created before life areas existed belong to the work area.
ALTER TABLE task ADD COLUMN area TEXT NOT NULL DEFAULT 'WORK';
ALTER TABLE task ALTER COLUMN area DROP DEFAULT;
