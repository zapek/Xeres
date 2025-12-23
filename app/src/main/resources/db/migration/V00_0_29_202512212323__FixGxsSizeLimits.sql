--
-- Change the GxS limits to make sure they fit.
-- It's 199000 per message, but we never know which field will
-- have that limit.
--
ALTER TABLE forum_group ALTER COLUMN description VARCHAR(199000);

ALTER TABLE channel_group ALTER COLUMN description VARCHAR(199000);
ALTER TABLE channel_message ALTER COLUMN content VARCHAR(199000);
ALTER TABLE channel_message ALTER COLUMN title VARCHAR(199000);
ALTER TABLE channel_message ALTER COLUMN comment VARCHAR(199000);

ALTER TABLE board_group ALTER COLUMN description VARCHAR(199000);
ALTER TABLE board_message ALTER COLUMN content VARCHAR(199000);
ALTER TABLE board_message ALTER COLUMN link VARCHAR(199000);

ALTER TABLE comment_message ALTER COLUMN comment VARCHAR(199000);

ALTER TABLE board_message ADD COLUMN image_width INT NOT NULL DEFAULT 0 AFTER image;
ALTER TABLE board_message ADD COLUMN image_height INT NOT NULL DEFAULT 0 AFTER image;

ALTER TABLE channel_message ADD COLUMN image_width INT NOT NULL DEFAULT 0 AFTER image;
ALTER TABLE channel_message ADD COLUMN image_height INT NOT NULL DEFAULT 0 AFTER image;
