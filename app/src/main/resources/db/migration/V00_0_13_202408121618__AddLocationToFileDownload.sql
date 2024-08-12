--
-- Add location to file downloads
--
ALTER TABLE file_download ADD COLUMN location_id BIGINT DEFAULT NULL AFTER size;