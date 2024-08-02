--
-- Add index for encrypted hashes
--
CREATE INDEX idx_encrypted_hash ON file(encrypted_hash);