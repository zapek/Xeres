--
-- Add participating locations to chat rooms
--
CREATE TABLE chat_room_locations
(
	chat_room_id BIGINT NOT NULL,
	locations_id  BIGINT NOT NULL
);