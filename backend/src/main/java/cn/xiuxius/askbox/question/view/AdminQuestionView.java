package cn.xiuxius.askbox.question.view;

/**
 * 管理端问题视图——额外包含 boxSlug、status、createdAt、answeredAt。
 */
public record AdminQuestionView(
        Long id, String boxSlug, String question, String status, String answer, long createdAt, Long answeredAt) {}
