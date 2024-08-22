--
-- Add availability field to locations
--
ALTER TABLE location ADD COLUMN availability ENUM ('available', 'busy', 'away') DEFAULT 'available' AFTER net_mode;