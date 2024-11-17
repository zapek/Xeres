--
-- Extend fingerprints to 32 bytes
--
ALTER TABLE profile ALTER COLUMN pgp_fingerprint VARBINARY(32);
