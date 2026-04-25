--
-- Migrate vote data because of missing converter
--

UPDATE vote_message SET type = 'up' WHERE type = 'down';
UPDATE vote_message SET type = 'down' WHERE type = 'none';
