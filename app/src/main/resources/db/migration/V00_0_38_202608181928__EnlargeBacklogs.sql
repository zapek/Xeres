--
-- Add more room to backlogs to store large messages
--
ALTER TABLE chat_backlog ALTER COLUMN message VARCHAR(1048576);
ALTER TABLE distant_chat_backlog ALTER COLUMN message VARCHAR(1048576);
