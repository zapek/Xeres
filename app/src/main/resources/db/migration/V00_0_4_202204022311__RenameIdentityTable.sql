--
-- Rename identity table
--
ALTER TABLE gxs_id_groups
	RENAME TO identity_groups;

ALTER TABLE chatrooms
	ALTER COLUMN gxs_id_group_id RENAME TO identity_group_id;