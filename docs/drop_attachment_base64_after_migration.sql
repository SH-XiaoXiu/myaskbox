DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM attachments
        WHERE object_key IS NULL OR object_key = ''
    ) THEN
        RAISE EXCEPTION 'attachments contains rows without object_key; run Base64AttachmentMigrationRunner first';
    END IF;
END $$;

UPDATE attachments
SET storage_type = 'S3';

ALTER TABLE attachments
    ALTER COLUMN object_key SET NOT NULL,
    DROP COLUMN content_base64;

DROP INDEX IF EXISTS ux_attachments_object_key_not_null;
CREATE UNIQUE INDEX ux_attachments_object_key ON attachments(object_key);
