--
-- Add DHT toggle to settings.
--
ALTER TABLE settings ADD dht_enabled BOOLEAN NOT NULL DEFAULT TRUE;
