package cn.xiuxius.askbox.question.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cn.xiuxius.askbox.common.R;
import cn.xiuxius.askbox.common.RequestLogFilter;
import cn.xiuxius.askbox.question.request.ReplyTokenAnswerRequest;
import cn.xiuxius.askbox.question.service.ReplyTokenService;
import cn.xiuxius.askbox.question.view.QuestionView;
import cn.xiuxius.askbox.question.view.ReplyTokenQuestionView;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/reply-tokens")
@RequiredArgsConstructor
@Tag(name = "公开接口", description = "一次性免登录回复问题")
public class ReplyTokenController {

    private final ReplyTokenService replyTokenService;

    @GetMapping("/{token}")
    @Operation(summary = "查看一次性回复链接对应的问题")
    public R<ReplyTokenQuestionView> get(@PathVariable String token) {
        return R.ok(replyTokenService.get(token));
    }

    @PostMapping("/{token}/answer")
    @Operation(summary = "使用一次性链接回答并发布问题")
    public R<QuestionView> answer(
            @PathVariable String token,
            @Valid @RequestBody ReplyTokenAnswerRequest request,
            HttpServletRequest httpRequest) {
        String ip = RequestLogFilter.resolveClientIp(httpRequest);
        String ua = httpRequest.getHeader("User-Agent");
        return R.ok(replyTokenService.answer(token, request.getAnswer(), ip, ua));
    }
}
