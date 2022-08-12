--
-- Rename table prefs to settings and add hidden services settings.
--
ALTER TABLE prefs RENAME TO settings;

ALTER TABLE settings ADD
(
	tor_socks_host VARCHAR(253) DEFAULT NULL,
	tor_socks_port INT NOT NULL DEFAULT 0,
	i2p_socks_host VARCHAR(253) DEFAULT NULL,
	i2p_socks_port INT NOT NULL DEFAULT 0
);