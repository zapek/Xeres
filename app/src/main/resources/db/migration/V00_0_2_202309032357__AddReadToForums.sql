--
-- Add read flag to forum messages
--
ALTER TABLE forum_message
	ADD
		(
		read BOOLEAN NOT NULL DEFAULT FALSE
		);