--
-- Add location version
--
ALTER TABLE location ADD COLUMN version VARCHAR(64) DEFAULT NULL AFTER last_connected;
