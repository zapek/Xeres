--
-- Add profile creation time
--
ALTER TABLE profile ADD COLUMN created TIMESTAMP(9) DEFAULT NULL AFTER pgp_identifier;
