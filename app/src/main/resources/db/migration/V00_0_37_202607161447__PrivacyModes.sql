--
-- Add privacy modes
--
ALTER TABLE identity_group ALTER COLUMN type ENUM ('other', 'own', 'friend') DEFAULT 'other';

ALTER TABLE settings ADD COLUMN upnp_activation_mode ENUM('off', 'private', 'on') DEFAULT 'private';
ALTER TABLE settings ADD COLUMN broadcast_discovery_activation_mode ENUM('off', 'private', 'on') DEFAULT 'private';
ALTER TABLE settings ADD COLUMN dns_lookup_enabled BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE settings ALTER COLUMN dht_enabled BOOLEAN NOT NULL DEFAULT FALSE;

-- Migrate old settings (private by default, off if they were turned off)
UPDATE settings SET upnp_activation_mode = CASE
	WHEN upnp_enabled = TRUE THEN 'private'
	ELSE 'off'
END WHERE lock = 1;

UPDATE settings SET broadcast_discovery_activation_mode = CASE
	WHEN broadcast_discovery_enabled = TRUE THEN 'private'
	ELSE 'off'
END WHERE lock = 1;

ALTER TABLE settings DROP column upnp_enabled;
ALTER TABLE settings DROP column broadcast_discovery_enabled;

