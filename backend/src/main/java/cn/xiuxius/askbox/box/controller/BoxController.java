package cn.xiuxius.askbox.box.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import cn.dev33.satoken.stp.StpUtil;
import cn.xiuxius.askbox.box.request.BoxProfileUpdateRequest;
import cn.xiuxius.askbox.boxuser.service.BoxUserService;
import cn.xiuxius.askbox.boxuser.view.BoxProfileView;
import cn.xiuxius.askbox.boxuser.view.BoxStatsView;
import cn.xiuxius.askbox.common.PageResult;
import cn.xiuxius.askbox.common.R;
import cn.xiuxius.askbox.common.RequestLogFilter;
import cn.xiuxius.askbox.question.request.AnswerQuestionRequest;
import cn.xiuxius.askbox.question.service.QuestionService;
import cn.xiuxius.askbox.question.view.PendingQuestionView;
import cn.xiuxius.askbox.question.view.QuestionView;
import cn.xiuxius.askbox.security.RequiresRole;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/** Box Owner 接口——提问箱主管理自己的箱子和问题。需要 BOX_OWNER 角色。 */
@RestController
@RequestMapping("/api/box")
@RequiredArgsConstructor
@RequiresRole("BOX_OWNER")
@Tag(name = "Box Owner 接口", description = "提问箱主管理自己的提问箱（需登录 + BOX_OWNER 角色）")
public class BoxController {

    private final BoxUserService boxUserService;
    private final QuestionService questionService;

    /** 获取当前登录用户 ID，并找到其提问箱 ID。 */
    private Long currentBoxUserId() {
        return boxUserService.getBoxIdByUserId(StpUtil.getLoginIdAsLong());
    }

    @GetMapping("/profile")
    @Operation(summary = "获取我的提问箱信息")
    public R<BoxProfileView> profile() {
        return R.ok(boxUserService.getProfileByUserId(StpUtil.getLoginIdAsLong()));
    }

    @PutMapping("/profile")
    @Operation(summary = "更新我的提问箱信息")
    public R<BoxProfileView> updateProfile(@Valid @RequestBody BoxProfileUpdateRequest request) {
        Long userId = StpUtil.getLoginIdAsLong();
        BoxProfileView updated = boxUserService.updateBox(
                userId,
                request.getSlug(),
                request.getDisplayName(),
                request.getDescription() == null ? "" : request.getDescription(),
                request.getAvatarObjectKey(),
                request.getBackgroundObjectKey(),
                request.getEmailNotifyEnabled());
        return R.ok(updated);
    }

    @GetMapping("/stats")
    @Operation(summary = "获取我的提问箱统计")
    public R<BoxStatsView> stats(@RequestParam(defaultValue = "Asia/Shanghai") String zone) {
        return R.ok(boxUserService.getStats(StpUtil.getLoginIdAsLong(), zone));
    }

    @GetMapping("/questions/pending")
    @Operation(summary = "获取待回答问题列表", description = "按 created_at ASC 排序（最早的在前）")
    public R<PageResult<PendingQuestionView>> pending(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") long page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") long pageSize) {
        return R.ok(questionService.getPending(currentBoxUserId(), page, pageSize));
    }

    @PostMapping("/questions/{id}/answer")
    @Operation(summary = "回答并发布问题", description = "回答问题后，状态变为 PUBLISHED，公开可见。")
    public R<QuestionView> answer(
            @Parameter(description = "问题 ID") @PathVariable Long id,
            @Valid @RequestBody AnswerQuestionRequest request,
            HttpServletRequest httpRequest) {
        String ip = RequestLogFilter.resolveClientIp(httpRequest);
        String ua = httpRequest.getHeader("User-Agent");
        Long userId = StpUtil.getLoginIdAsLong();
        return R.ok(questionService.answer(currentBoxUserId(), id, request.getAnswer(), userId, ip, ua));
    }

    @PostMapping("/questions/{id}/dismiss")
    @Operation(summary = "驳回问题", description = "驳回后问题不再公开，可随后删除。")
    public R<Void> dismiss(@Parameter(description = "问题 ID") @PathVariable Long id) {
        questionService.dismiss(currentBoxUserId(), id);
        return R.ok();
    }

    @DeleteMapping("/questions/{id}")
    @Operation(summary = "删除已驳回的问题", description = "物理删除已驳回的问题及其回答。")
    public R<Void> delete(@Parameter(description = "问题 ID") @PathVariable Long id) {
        questionService.delete(currentBoxUserId(), id);
        return R.ok();
    }

    @GetMapping("/questions/history")
    @Operation(summary = "历史问题列表", description = "获取指定状态的历史问题（PUBLISHED 或 DISMISSED），含回答。")
    public R<PageResult<QuestionView>> history(
            @Parameter(description = "状态：PUBLISHED 或 DISMISSED") @RequestParam String status,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") long page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "20") long pageSize) {
        return R.ok(questionService.getHistory(currentBoxUserId(), status, page, pageSize));
    }
}
