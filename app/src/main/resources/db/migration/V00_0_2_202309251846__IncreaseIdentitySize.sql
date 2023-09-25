--
-- Increase identity image size
--
ALTER TABLE identity_group ALTER COLUMN image VARBINARY(131072) DEFAULT NULL;
