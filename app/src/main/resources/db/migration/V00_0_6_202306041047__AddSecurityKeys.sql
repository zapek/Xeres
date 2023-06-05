--
-- Change security keys system
--
ALTER TABLE gxs_groups
	DROP COLUMN admin_private_key, admin_public_key, publishing_private_key, publishing_public_key;

CREATE TABLE gxs_groups_private_keys
(
	gxs_groups_id BIGINT     NOT NULL,
	key_id        BINARY(16) NOT NULL,
	flags         INT        NOT NULL DEFAULT 0,
	valid_from    TIMESTAMP  NOT NULL,
	valid_to      TIMESTAMP,
	data          VARBINARY(16384)
);

CREATE TABLE gxs_groups_public_keys
(
	gxs_groups_id BIGINT     NOT NULL,
	key_id        BINARY(16) NOT NULL,
	flags         INT        NOT NULL DEFAULT 0,
	valid_from    TIMESTAMP  NOT NULL,
	valid_to      TIMESTAMP,
	data          VARBINARY(16384)
);