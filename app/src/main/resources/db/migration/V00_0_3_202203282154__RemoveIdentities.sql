--
-- Remove identities and use gxs_id_groups instead
--
ALTER TABLE gxs_id_groups
	DROP COLUMN image_type;

ALTER TABLE gxs_id_groups
	ALTER COLUMN image_data RENAME TO image;

ALTER TABLE gxs_id_groups
	ADD COLUMN type ENUM ('other', 'own', 'friend', 'banned') DEFAULT 'other';

CREATE INDEX idx_type ON gxs_id_groups (type);

DROP TABLE identities;

ALTER TABLE chatrooms
	ALTER COLUMN identity_id RENAME TO gxs_id_group_id;
