--
-- Remove the useless fields in GxsGroupItem and GxsMessageItem.
-- Add a field to handle multi versioning.
-- Fix identity service string.
-- Speed up forum, board and channel counting
--
ALTER TABLE gxs_group DROP COLUMN status, service_string, original_gxs_id;

ALTER TABLE gxs_message DROP COLUMN status, child, service_string;

ALTER TABLE gxs_message ADD COLUMN hidden BOOLEAN NOT NULL DEFAULT FALSE AFTER flags;

CREATE INDEX idx_message_hidden ON gxs_message (hidden);

ALTER TABLE identity_group ADD COLUMN overall_score INT NOT NULL DEFAULT 5 AFTER type;
ALTER TABLE identity_group ADD COLUMN identity_score INT NOT NULL DEFAULT 5 AFTER overall_score;
ALTER TABLE identity_group ADD COLUMN own_opinion INT NOT NULL DEFAULT 0 AFTER identity_score;
ALTER TABLE identity_group ADD COLUMN peer_opinion INT NOT NULL DEFAULT 0 AFTER own_opinion;

ALTER TABLE identity_group ADD COLUMN validation_attempt INT NOT NULL DEFAULT 0 AFTER peer_opinion;
ALTER TABLE identity_group ADD COLUMN last_validation TIMESTAMP(9) AFTER validation_attempt;

ALTER TABLE identity_group ADD COLUMN last_usage TIMESTAMP(9) AFTER last_validation;

CREATE INDEX idx_forum_message_read ON forum_message (read);
CREATE INDEX idx_board_message_read ON board_message (read);
CREATE INDEX idx_channel_message_read ON channel_message (read);