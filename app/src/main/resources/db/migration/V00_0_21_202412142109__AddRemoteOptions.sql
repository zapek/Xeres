--
-- Add remote options
--
ALTER TABLE settings ADD COLUMN remote_enabled BOOLEAN NOT NULL DEFAULT FALSE AFTER remote_password;
ALTER TABLE settings ADD COLUMN upnp_remote_enabled BOOLEAN NOT NULL DEFAULT TRUE AFTER remote_enabled;