--
-- Add last activity column to know
-- when remote groups were updated by friends.
--
ALTER TABLE gxs_group ALTER COLUMN last_posted RENAME TO last_updated;
ALTER TABLE gxs_group ADD COLUMN last_activity TIMESTAMP(9) NOT NULL DEFAULT parsedatetime('1970-01-01 00:00:00', 'yyyy-MM-dd hh:mm:ss') AFTER visible_message_count;
ALTER TABLE gxs_group ADD COLUMN last_statistics TIMESTAMP(9) NOT NULL DEFAULT parsedatetime('1970-01-01 00:00:00', 'yyyy-MM-dd hh:mm:ss') AFTER last_activity;

CREATE INDEX idx_gxs_group_last_statistics ON gxs_group (last_statistics);