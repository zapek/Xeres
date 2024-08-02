--
-- Add encrypted hashes to files
--
ALTER TABLE file ADD COLUMN encrypted_hash BINARY(20) AFTER hash;