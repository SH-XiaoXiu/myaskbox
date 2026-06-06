-- ============================================================
-- V3: 提问箱、问题、回答表
-- ============================================================

-- 提问箱表
CREATE TABLE box_user (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT NOT NULL UNIQUE REFERENCES sys_user(id),
    slug        VARCHAR(50) NOT NULL UNIQUE,
    display_name VARCHAR(100),
    description TEXT,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 问题表（引用头像、含 IP/UA）
CREATE TABLE questions (
    id            BIGSERIAL PRIMARY KEY,
    box_user_id   BIGINT NOT NULL REFERENCES box_user(id),
    avatar_id     BIGINT NOT NULL REFERENCES avatars(id),
    question      TEXT NOT NULL,
    status        VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    ip            VARCHAR(50),
    user_agent    TEXT,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_questions_box_status ON questions(box_user_id, status);
CREATE INDEX idx_questions_box_published ON questions(box_user_id, created_at DESC) WHERE status = 'PUBLISHED';
CREATE INDEX idx_questions_box_pending ON questions(box_user_id, created_at ASC) WHERE status = 'PENDING';

-- 回答表（独立表，含 IP/UA）
CREATE TABLE answers (
    id            BIGSERIAL PRIMARY KEY,
    question_id   BIGINT NOT NULL UNIQUE REFERENCES questions(id),
    content       TEXT NOT NULL,
    answered_by   BIGINT NOT NULL REFERENCES sys_user(id),
    ip            VARCHAR(50),
    user_agent    TEXT,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_answers_question ON answers(question_id);

-- 种子数据: 为 admin 用户创建提问箱
INSERT INTO box_user (user_id, slug, display_name, description) VALUES
(1, 'admin', 'Admin 的提问箱', '欢迎匿名提问！');
