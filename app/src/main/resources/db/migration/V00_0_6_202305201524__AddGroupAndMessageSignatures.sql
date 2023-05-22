--
-- Add signatures to groups.
--
ALTER TABLE gxs_groups ADD
(
	admin_signature  VARBINARY(512) DEFAULT NULL,
	author_signature VARBINARY(512) DEFAULT NULL
);

ALTER TABLE gxs_messages ADD
(
	publish_signature VARBINARY(512) DEFAULT NULL,
	author_signature  VARBINARY(512) DEFAULT NULL
);
