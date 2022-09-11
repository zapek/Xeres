--
-- Add autostart toggle to settings.
--
ALTER TABLE settings ADD auto_start_enabled BOOLEAN NOT NULL DEFAULT FALSE;
