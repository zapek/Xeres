--
-- Add profile validation to identities
--
ALTER TABLE identity_group ADD COLUMN profile_id BIGINT DEFAULT NULL AFTER id;
ALTER TABLE identity_group ADD COLUMN next_validation TIMESTAMP(9) DEFAULT NULL AFTER profile_signature;

UPDATE identity_group SET next_validation = PARSEDATETIME('1970-01-01 00:00:00', 'yyyy-MM-dd hh:mm:ss') WHERE id != 1 AND profile_signature IS NOT NULL