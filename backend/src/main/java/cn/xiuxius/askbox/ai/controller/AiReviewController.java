package cn.xiuxius.askbox.ai.controller;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cn.xiuxius.askbox.ai.request.AiReviewBatchRequest;
import cn.xiuxius.askbox.ai.service.AiReviewService;
import cn.xiuxius.askbox.ai.view.AiReviewView;
import cn.xiuxius.askbox.common.R;
import cn.xiuxius.askbox.common.ratelimit.RateLimit;
import cn.xiuxius.askbox.common.ratelimit.RateLimitType;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/ai-reviews")
@RequiredArgsConstructor
@Tag(name = "AI点评接口", description = "公开AI点评查询")
public class AiReviewController {
    private final AiReviewService aiReviewService;

    @PostMapping("/batch")
    @Operation(summary = "批量查询公开AI点评")
    @RateLimit(
            types = {RateLimitType.IP, RateLimitType.USER_AGENT},
            capacity = 120,
            refillTokens = 120,
            timeWindowSeconds = 60)
    public R<List<AiReviewView>> batch(@Valid @RequestBody AiReviewBatchRequest request) {
        return R.ok(aiReviewService.batchSucceededPublic(request.questionIds()));
    }
}
