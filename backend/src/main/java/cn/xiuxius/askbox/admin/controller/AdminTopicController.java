package cn.xiuxius.askbox.admin.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import cn.xiuxius.askbox.common.PageResult;
import cn.xiuxius.askbox.common.R;
import cn.xiuxius.askbox.security.RequiresRole;
import cn.xiuxius.askbox.topic.service.TopicService;
import cn.xiuxius.askbox.topic.view.AdminTopicView;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/topics")
@RequiredArgsConstructor
@RequiresRole("ADMIN")
@Tag(name = "系统管理接口", description = "系统管理员全局管理（需 ADMIN 角色）")
public class AdminTopicController {
    private final TopicService topicService;

    @GetMapping
    @Operation(summary = "话题列表")
    public R<PageResult<AdminTopicView>> listTopics(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(required = false) Long boxUserId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword) {
        return R.ok(topicService.listAdminTopics(page, pageSize, boxUserId, status, keyword));
    }

    @PostMapping("/{id}/close")
    @Operation(summary = "关闭任意话题")
    public R<AdminTopicView> closeTopic(@PathVariable Long id) {
        return R.ok(topicService.adminClose(id));
    }
}
