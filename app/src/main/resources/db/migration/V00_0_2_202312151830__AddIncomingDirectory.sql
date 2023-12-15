--
-- Add incoming directory to settings
--
ALTER TABLE settings ADD COLUMN incoming_directory VARCHAR(1024) DEFAULT NULL AFTER auto_start_enabled;