package cn.xiuxius.askbox.question.event;

public record QuestionSubmittedEvent(Long questionId, String origin) {}
