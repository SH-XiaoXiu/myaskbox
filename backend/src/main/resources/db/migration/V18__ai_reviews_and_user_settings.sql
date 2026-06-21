CREATE TABLE user_setting (
    user_id    BIGINT NOT NULL REFERENCES sys_user(id) ON DELETE CASCADE,
    key        VARCHAR(100) NOT NULL,
    value      TEXT,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (user_id, key)
);

INSERT INTO user_setting (user_id, key, value)
SELECT id, 'topic.active_limit', COALESCE(topic_active_limit, 5)::TEXT
FROM sys_user
ON CONFLICT (user_id, key) DO UPDATE SET value = EXCLUDED.value, updated_at = NOW();

ALTER TABLE sys_user DROP COLUMN topic_active_limit;

CREATE TABLE ai_reviews (
    id             BIGSERIAL PRIMARY KEY,
    question_id    BIGINT NOT NULL UNIQUE REFERENCES questions(id) ON DELETE CASCADE,
    box_user_id    BIGINT NOT NULL REFERENCES box_user(id) ON DELETE CASCADE,
    answer_id      BIGINT NOT NULL REFERENCES answers(id) ON DELETE CASCADE,
    status         VARCHAR(20) NOT NULL,
    content        TEXT,
    error_message  TEXT,
    model          VARCHAR(100),
    prompt_version VARCHAR(50),
    trigger_type   VARCHAR(20) NOT NULL,
    triggered_by   BIGINT REFERENCES sys_user(id) ON DELETE SET NULL,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    completed_at   TIMESTAMPTZ
);

CREATE INDEX idx_ai_reviews_box_status ON ai_reviews(box_user_id, status, updated_at DESC);
CREATE INDEX idx_ai_reviews_answer ON ai_reviews(answer_id);

CREATE TABLE ai_box_profiles (
    box_user_id    BIGINT PRIMARY KEY REFERENCES box_user(id) ON DELETE CASCADE,
    style_summary  TEXT,
    sample_count   INT NOT NULL DEFAULT 0,
    version        INT NOT NULL DEFAULT 1,
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE ai_call_logs (
    id                BIGSERIAL PRIMARY KEY,
    question_id       BIGINT REFERENCES questions(id) ON DELETE SET NULL,
    ai_review_id      BIGINT REFERENCES ai_reviews(id) ON DELETE SET NULL,
    box_user_id       BIGINT REFERENCES box_user(id) ON DELETE SET NULL,
    trigger_type      VARCHAR(20),
    triggered_by      BIGINT REFERENCES sys_user(id) ON DELETE SET NULL,
    stage             VARCHAR(50) NOT NULL,
    success           BOOLEAN NOT NULL DEFAULT FALSE,
    model             VARCHAR(100),
    base_url          TEXT,
    request_payload   TEXT,
    response_payload  TEXT,
    error_message     TEXT,
    duration_ms       BIGINT,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_ai_call_logs_question_created ON ai_call_logs(question_id, created_at DESC);
CREATE INDEX idx_ai_call_logs_review_created ON ai_call_logs(ai_review_id, created_at DESC);
CREATE INDEX idx_ai_call_logs_box_created ON ai_call_logs(box_user_id, created_at DESC);
