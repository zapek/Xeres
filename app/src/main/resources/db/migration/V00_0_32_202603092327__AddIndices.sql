--
-- Indices to speed up lookup
--

CREATE INDEX idx_message_published ON gxs_message (published);

CREATE INDEX idx_location_last_connected ON location (last_connected);

CREATE INDEX idx_group_last_statistics ON gxs_group (last_statistics);

CREATE INDEX idx_identity_next_validation ON identity_group (next_validation);
