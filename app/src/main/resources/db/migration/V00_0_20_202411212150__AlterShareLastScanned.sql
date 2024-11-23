--
-- Make sure we don't have null values for last scanned because the criteria API doesn't support nullsFirst
--
UPDATE share SET last_scanned = parsedatetime('1970-01-01 00:00:00', 'yyyy-MM-dd hh:mm:ss') WHERE last_scanned IS NULL;

ALTER TABLE share ALTER COLUMN last_scanned TIMESTAMP(9) NOT NULL DEFAULT parsedatetime('1970-01-01 00:00:00', 'yyyy-MM-dd hh:mm:ss');
