package cn.xiuxius.askbox.ai.view;

public record AiReviewView(
        Long id,
        Long questionId,
        String status,
        String content,
        String errorMessage,
        String triggerType,
        Long completedAt) {}
