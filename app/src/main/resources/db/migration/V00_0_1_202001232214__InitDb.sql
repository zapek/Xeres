--
-- Database creation
--
-- Avoid touching this file if unnecessary (even comments) as this will trigger
-- a flyway migration. Migrations will be consistently used and this file won't
-- be touched anymore when we reach 1.0.0
--
-- See https://h2database.com/html/datatypes.html for the data types
--
CREATE TABLE profiles
(
	id                  IDENTITY    NOT NULL PRIMARY KEY,
	name                VARCHAR(64) NOT NULL,
	pgp_identifier      BIGINT      NOT NULL UNIQUE,
	pgp_fingerprint     BINARY(20)  NOT NULL,
	pgp_public_key_data VARBINARY(16384),
	accepted            BOOLEAN     NOT NULL                                      DEFAULT false,
	trust               ENUM ('unknown', 'never', 'marginal', 'full', 'ultimate') DEFAULT 'unknown'
);

CREATE TABLE locations
(
	id                  IDENTITY    NOT NULL PRIMARY KEY,
	profile_id          BIGINT      NOT NULL,
	name                VARCHAR(64) NOT NULL,
	location_identifier BINARY(16)  NOT NULL UNIQUE,
	connected           BOOLEAN     NOT NULL                                            DEFAULT false,
	discoverable        BOOLEAN     NOT NULL                                            DEFAULT true,
	dht                 BOOLEAN     NOT NULL                                            DEFAULT true,
	net_mode            ENUM ('unknown', 'udp', 'upnp', 'ext', 'hidden', 'unreachable') DEFAULT 'unknown',
	last_connected      TIMESTAMP
);

CREATE TABLE connections
(
	id             IDENTITY     NOT NULL PRIMARY KEY,
	location_id    BIGINT       NOT NULL,
	type           ENUM ('invalid', 'ipv4', 'ipv6', 'tor', 'i2p'),
	address        VARCHAR(128) NOT NULL,
	last_connected TIMESTAMP,
	external       BOOLEAN      NOT NULL
);

CREATE TABLE identities
(
	id     IDENTITY NOT NULL PRIMARY KEY,
	gxs_id BIGINT   NOT NULL,
	type   ENUM ('signed', 'anonymous', 'friend')
);

CREATE TABLE prefs
(
	lock                      TINYINT NOT NULL DEFAULT 1,

	pgp_private_key_data      VARBINARY(16384) DEFAULT NULL,

	location_private_key_data VARBINARY(16384) DEFAULT NULL,
	location_public_key_data  VARBINARY(16384) DEFAULT NULL,
	location_certificate      VARBINARY(16384) DEFAULT NULL,

	CONSTRAINT PK_T1 PRIMARY KEY (lock),
	CONSTRAINT CK_T1_LOCKED CHECK (lock = 1)
);

CREATE TABLE gxs_client_updates
(
	id           IDENTITY NOT NULL PRIMARY KEY,
	location_id  BIGINT   NOT NULL,
	service_type INT      NOT NULL,
	last_synced  TIMESTAMP
);
CREATE INDEX idx_location_service ON gxs_client_updates (location_id, service_type);

CREATE TABLE gxs_service_settings
(
	id           IDENTITY NOT NULL PRIMARY KEY,
	last_updated TIMESTAMP
);

CREATE TABLE gxs_groups
(
	id                     IDENTITY     NOT NULL PRIMARY KEY,
	gxs_id                 BINARY(16)   NOT NULL UNIQUE,
	original_gxs_id        BINARY(16),
	name                   VARCHAR(512) NOT NULL,
	diffusion_flags        INT          NOT NULL                                                                                   DEFAULT 0,
	signature_flags        INT          NOT NULL                                                                                   DEFAULT 0,
	published              TIMESTAMP,
	author                 BINARY(16),
	circle_id              BINARY(16),
	circle_type            ENUM ('unknown', 'public', 'external', 'your_friends_only', 'local', 'external_self', 'your_eyes_only') DEFAULT 'unknown',
	authentication_flags   INT          NOT NULL                                                                                   DEFAULT 0,
	parent_id              BINARY(16),
	subscribe_flags        INT          NOT NULL                                                                                   DEFAULT 0,
	popularity             INT          NOT NULL                                                                                   DEFAULT 0,
	visible_message_count  INT          NOT NULL                                                                                   DEFAULT 0,
	last_posted            TIMESTAMP,
	status                 INT          NOT NULL                                                                                   DEFAULT 0,
	service_string         VARCHAR(512),
	originator             BINARY(16),
	internal_circle        BINARY(16),
	admin_private_key      VARBINARY(16384),
	admin_public_key       VARBINARY(16384),
	publishing_private_key VARBINARY(16384),
	publishing_public_key  VARBINARY(16384)
);

CREATE TABLE gxs_id_groups
(
	id                IDENTITY NOT NULL PRIMARY KEY,
	profile_hash      BINARY(20),
	profile_signature VARBINARY(2048)
);

CREATE TABLE chatrooms
(
	id          IDENTITY     NOT NULL PRIMARY KEY,
	room_id     BIGINT       NOT NULL,
	identity_id BIGINT       NOT NULL,
	name        VARCHAR(256) NOT NULL,
	topic       VARCHAR(256) NOT NULL,
	flags       INT          NOT NULL DEFAULT 0,
	subscribed  BOOLEAN      NOT NULL DEFAULT true,
	joined      BOOLEAN      NOT NULL DEFAULT false
);

INSERT INTO prefs (lock)
VALUES (1);
