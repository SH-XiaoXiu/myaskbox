-- ============================================================
-- V6: 移除当前 schema 下全部物理外键
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
