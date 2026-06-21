ALTER TABLE like_counts DROP CONSTRAINT IF EXISTS ck_like_counts_target_type;

ALTER TABLE like_counts
    ADD CONSTRAINT ck_like_counts_target_type CHECK (target_type IN ('QUESTION', 'ANSWER', 'AI_REVIEW'));
