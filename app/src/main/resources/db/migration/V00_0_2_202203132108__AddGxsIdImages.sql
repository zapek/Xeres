--
-- Add image avatar to gxs id
--
ALTER TABLE gxs_id_groups
	ADD (
		image_type TINYINT NOT NULL DEFAULT 0,
		image_data VARBINARY(65536) DEFAULT NULL
		);