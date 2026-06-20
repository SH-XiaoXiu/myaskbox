-- ============================================================
-- V16: Like counts for public questions and answers
-- ============================================================

CREATE TABLE like_counts (
    id          BIGSERIAL PRIMARY KEY,
    target_type VARCHAR(20) NOT NULL,
    target_id   BIGINT NOT NULL,
    like_count  BIGINT NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_like_counts_target UNIQUE (target_type, target_id),
    CONSTRAINT ck_like_counts_target_type CHECK (target_type IN ('QUESTION', 'ANSWER')),
    CONSTRAINT ck_like_counts_non_negative CHECK (like_count >= 0)
);

CREATE INDEX idx_like_counts_target ON like_counts(target_type, target_id);
