package cn.xiuxius.askbox.admin.controller;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import cn.xiuxius.askbox.answer.service.AnswerService;
import cn.xiuxius.askbox.common.PageResult;
import cn.xiuxius.askbox.common.R;
import cn.xiuxius.askbox.question.service.QuestionService;
import cn.xiuxius.askbox.question.view.AdminQuestionView;
import cn.xiuxius.askbox.security.RequiresRole;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@RequiresRole("ADMIN")
@Tag(name = "系统管理接口", description = "系统管理员全局管理（需 ADMIN 角色）")
public class AdminQuestionController {

    private final QuestionService questionService;
    private final AnswerService answerService;

    @GetMapping("/questions")
    @Operation(summary = "所有问题列表", description = "可按提问箱、状态、关键词筛选。返回含 boxSlug 的管理视图。")
    public R<PageResult<AdminQuestionView>> listAllQuestions(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(required = false) Long boxUserId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword) {
        return R.ok(questionService.listAllAdmin(page, pageSize, boxUserId, status, keyword));
    }

    @GetMapping("/questions/{id}")
    @Operation(summary = "问题详情", description = "查看任意问题的详情（含回答）。")
    public R<AdminQuestionView> getQuestion(@PathVariable Long id) {
        return R.ok(questionService.getAdminById(id));
    }

    @DeleteMapping("/questions/{id}")
    @Operation(summary = "强制删除任意问题")
    public R<Void> forceDeleteQuestion(@PathVariable Long id) {
        questionService.forceDelete(id);
        return R.ok();
    }

    @DeleteMapping("/answers/{id}")
    @Operation(summary = "删除任意回答")
    public R<Void> deleteAnswer(@PathVariable Long id) {
        answerService.delete(id);
        return R.ok();
    }
}
