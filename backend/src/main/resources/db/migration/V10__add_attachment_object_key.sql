ALTER TABLE attachments
    ADD COLUMN object_key VARCHAR(512);

CREATE UNIQUE INDEX ux_attachments_object_key_not_null
    ON attachments(object_key)
    WHERE object_key IS NOT NULL AND object_key <> '';
