--
-- Adjust message backlog sizes
--

ALTER TABLE chat_backlog ALTER COLUMN message VARCHAR(300000);
ALTER TABLE distant_chat_backlog ALTER COLUMN message VARCHAR(300000);
ALTER TABLE chat_room_backlog ALTER COLUMN message VARCHAR(40000);
