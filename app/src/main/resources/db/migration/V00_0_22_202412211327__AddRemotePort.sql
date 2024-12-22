--
-- Add remote port
--
ALTER TABLE settings ADD COLUMN remote_port INTEGER NOT NULL DEFAULT 0 AFTER remote_enabled;