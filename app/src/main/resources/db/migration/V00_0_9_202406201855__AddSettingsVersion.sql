--
-- Add settings version
--
ALTER TABLE settings ADD COLUMN version INT NOT NULL DEFAULT 0 AFTER lock;