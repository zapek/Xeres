--
-- Add new enum types
--
ALTER TABLE file ALTER COLUMN type ENUM ('any', 'audio', 'archive', 'document', 'picture', 'program', 'video', 'subtitles', 'collection', 'directory') DEFAULT 'any';
