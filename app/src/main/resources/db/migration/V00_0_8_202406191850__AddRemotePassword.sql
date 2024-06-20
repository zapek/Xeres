--
-- Add remote password
--
ALTER TABLE settings ADD COLUMN remote_password VARCHAR(64) DEFAULT NULL AFTER incoming_directory;
