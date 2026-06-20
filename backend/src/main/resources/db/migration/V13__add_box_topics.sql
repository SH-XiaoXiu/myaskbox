-- ============================================================
-- V13: Box topics and question linkage
-- ============================================================

CREATE TABLE box_topics (
    id           BIGSERIAL PRIMARY KEY,
    box_user_id  BIGINT NOT NULL REFERENCES box_user(id),
    code         VARCHAR(24) NOT NULL UNIQUE,
    title        VARCHAR(80) NOT NULL,
    description  TEXT,
    status       VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    expires_at   TIMESTAMPTZ,
    closed_at    TIMESTAMPTZ,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_box_topics_box_recent ON box_topics(box_user_id, created_at DESC);
CREATE INDEX idx_box_topics_box_active ON box_topics(box_user_id, status, expires_at, created_at DESC);

ALTER TABLE questions
    ADD COLUMN topic_id BIGINT REFERENCES box_topics(id);

CREATE INDEX idx_questions_box_topic_status_created
    ON questions(box_user_id, topic_id, status, created_at DESC);
