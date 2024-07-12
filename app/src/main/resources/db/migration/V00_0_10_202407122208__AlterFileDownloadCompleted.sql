--
-- Allow to mark file downloads as completed
--
ALTER TABLE file_download ADD COLUMN completed BOOLEAN NOT NULL DEFAULT FALSE AFTER chunk_map;
