--
-- Add message updates to client updates
--
CREATE TABLE gxs_client_updates_messages
(
	gxs_client_update_id BIGINT     NOT NULL,
	identifier           BINARY(16) NOT NULL, -- normal name would be 'gxs_id' but hibernate doesn't let us use @AttributeOverride for an embeddable key and basic type (it wants the value as an embeddable type too then)
	updated              TIMESTAMP  NOT NULL
);