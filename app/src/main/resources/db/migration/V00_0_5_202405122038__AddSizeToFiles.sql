--
-- Add size to files
--
ALTER TABLE file ADD COLUMN size BIGINT NOT NULL DEFAULT 0 AFTER name;
