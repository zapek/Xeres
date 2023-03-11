--
-- Add subscribed field to groups, remove the subscription flag.
--
ALTER TABLE gxs_groups ADD subscribed BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE gxs_groups DROP COLUMN subscribe_flags;

-- set all identities to subscribed, as they should be
UPDATE gxs_groups SET subscribed = true;
