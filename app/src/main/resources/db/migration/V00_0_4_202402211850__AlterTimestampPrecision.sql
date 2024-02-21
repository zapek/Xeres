--
-- Change precision from default microseconds to nanoseconds for all timestamps
-- so that comparison problems don't arise.
--
ALTER TABLE file ALTER COLUMN modified TIMESTAMP(9);
ALTER TABLE share ALTER COLUMN last_scanned TIMESTAMP(9);
ALTER TABLE location ALTER COLUMN last_connected TIMESTAMP(9);
ALTER TABLE connection ALTER COLUMN last_connected TIMESTAMP(9);
ALTER TABLE gxs_client_update ALTER COLUMN last_synced TIMESTAMP(9);
ALTER TABLE gxs_client_update_messages ALTER COLUMN updated TIMESTAMP(9);
ALTER TABLE gxs_service_setting ALTER COLUMN last_updated TIMESTAMP(9);
ALTER TABLE gxs_group ALTER COLUMN published TIMESTAMP(9);
ALTER TABLE gxs_group ALTER COLUMN last_posted TIMESTAMP(9);
ALTER TABLE gxs_group_private_keys ALTER COLUMN valid_from TIMESTAMP(9);
ALTER TABLE gxs_group_private_keys ALTER COLUMN valid_to TIMESTAMP(9);
ALTER TABLE gxs_group_public_keys ALTER COLUMN valid_from TIMESTAMP(9);
ALTER TABLE gxs_group_public_keys ALTER COLUMN valid_to TIMESTAMP(9);
ALTER TABLE gxs_message ALTER COLUMN published TIMESTAMP(9);
ALTER TABLE gxs_message ALTER COLUMN child TIMESTAMP(9);

