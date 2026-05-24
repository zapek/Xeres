--
-- Set LastUsage identity default to EPOCH
--
-- noinspection SqlWithoutWhere
UPDATE identity_group SET last_usage = now();
ALTER TABLE identity_group ALTER COLUMN last_usage TIMESTAMP(9) NOT NULL DEFAULT now();