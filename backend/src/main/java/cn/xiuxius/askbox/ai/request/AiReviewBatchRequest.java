package cn.xiuxius.askbox.ai.request;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;

public record AiReviewBatchRequest(@NotEmpty(message = "问题ID不能为空") List<Long> questionIds) {}
