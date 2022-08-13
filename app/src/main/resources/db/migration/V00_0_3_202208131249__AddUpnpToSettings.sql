--
-- Add UPNP toggle to settings.
--
ALTER TABLE settings ADD
(
	upnp_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    broadcast_discovery_enabled BOOLEAN NOT NULL DEFAULT TRUE
);