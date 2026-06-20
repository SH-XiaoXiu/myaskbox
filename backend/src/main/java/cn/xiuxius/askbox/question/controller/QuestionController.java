package cn.xiuxius.askbox.question.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import cn.xiuxius.askbox.common.PageResult;
import cn.xiuxius.askbox.common.R;
import cn.xiuxius.askbox.common.RequestLogFilter;
import cn.xiuxius.askbox.common.ratelimit.RateLimit;
import cn.xiuxius.askbox.common.ratelimit.RateLimitType;
import cn.xiuxius.askbox.question.request.SubmitQuestionRequest;
import cn.xiuxius.askbox.question.service.QuestionService;
import cn.xiuxius.askbox.question.view.QuestionView;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/** 公开接口——匿名访客向指定提问箱提交问题和浏览已发布内容。 */
@RestController
@RequestMapping("/api/boxes/{slug}/questions")
@RequiredArgsConstructor
@Tag(name = "公开接口", description = "匿名提问与查看（无需登录）")
public class QuestionController {

    private final QuestionService questionService;

    @PostMapping
    @Operation(summary = "提交匿名问题", description = "向指定提问箱提交一个匿名问题。需选择预设头像。")
    @RateLimit(
            types = {RateLimitType.IP, RateLimitType.USER_AGENT, RateLimitType.CUSTOM},
            key = "#slug",
            capacity = 30,
            refillTokens = 30,
            timeWindowSeconds = 60)
    public R<Void> submit(
            @Parameter(description = "提问箱 slug") @PathVariable String slug,
            @Valid @RequestBody SubmitQuestionRequest request,
            HttpServletRequest httpRequest) {
        // 获取客户端 IP 和 User-Agent 用于审计
        String ip = RequestLogFilter.resolveClientIp(httpRequest);
        String ua = httpRequest.getHeader("User-Agent");
        questionService.submit(
                slug,
                request.getAttachmentId(),
                request.getQuestion(),
                request.getTopicCode(),
                ip,
                ua,
                requestOrigin(httpRequest));
        return R.ok();
    }

    private String requestOrigin(HttpServletRequest request) {
        String origin = request.getHeader("Origin");
        if (origin != null && !origin.isBlank()) {
            return origin;
        }
        String forwardedProto = request.getHeader("X-Forwarded-Proto");
        String forwardedHost = request.getHeader("X-Forwarded-Host");
        if (forwardedProto != null && !forwardedProto.isBlank() && forwardedHost != null && !forwardedHost.isBlank()) {
            return forwardedProto + "://" + forwardedHost;
        }
        return request.getScheme() + "://" + request.getServerName()
                + ((request.getServerPort() == 80 || request.getServerPort() == 443)
                        ? ""
                        : ":" + request.getServerPort());
    }

    @GetMapping
    @Operation(summary = "获取已发布 Q&A 列表", description = "按分页返回指定提问箱中已公开发布的问题和回答。")
    public R<PageResult<QuestionView>> list(
            @Parameter(description = "提问箱 slug") @PathVariable String slug,
            @Parameter(description = "话题 code") @RequestParam(required = false) String topicCode,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") long page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") long pageSize) {
        return R.ok(questionService.getPublished(slug, topicCode, page, pageSize));
    }
}
