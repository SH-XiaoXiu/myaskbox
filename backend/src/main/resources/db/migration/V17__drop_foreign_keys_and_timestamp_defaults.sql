-- ============================================================
-- V17: Drop physical foreign keys and application-owned timestamp defaults
-- ============================================================

DO $$
DECLARE
    fk RECORD;
BEGIN
    FOR fk IN
        SELECT
            n.nspname AS schema_name,
            c.relname AS table_name,
            con.conname AS constraint_name
        FROM pg_constraint con
        JOIN pg_class c ON c.oid = con.conrelid
        JOIN pg_namespace n ON n.oid = c.relnamespace
        WHERE con.contype = 'f'
          AND n.nspname = current_schema()
    LOOP
        EXECUTE format(
            'ALTER TABLE %I.%I DROP CONSTRAINT %I',
            fk.schema_name,
            fk.table_name,
            fk.constraint_name
        );
    END LOOP;
END $$;

DO $$
DECLARE
    column_name TEXT;
    table_name TEXT;
BEGIN
    FOR table_name, column_name IN
        SELECT c.table_name, c.column_name
        FROM information_schema.columns c
        WHERE c.table_schema = current_schema()
          AND c.column_name IN ('created_at', 'updated_at')
          AND c.column_default IS NOT NULL
    LOOP
        EXECUTE format(
            'ALTER TABLE %I.%I ALTER COLUMN %I DROP DEFAULT',
            current_schema(),
            table_name,
            column_name
        );
    END LOOP;
END $$;
