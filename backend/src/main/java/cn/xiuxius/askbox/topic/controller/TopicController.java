package cn.xiuxius.askbox.topic.controller;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cn.dev33.satoken.stp.StpUtil;
import cn.xiuxius.askbox.boxuser.service.BoxUserService;
import cn.xiuxius.askbox.common.R;
import cn.xiuxius.askbox.security.RequiresRole;
import cn.xiuxius.askbox.topic.request.TopicCreateRequest;
import cn.xiuxius.askbox.topic.service.TopicService;
import cn.xiuxius.askbox.topic.view.TopicView;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/box/topics")
@RequiredArgsConstructor
@RequiresRole("BOX_OWNER")
@Tag(name = "Box Owner 接口", description = "提问箱主话题管理")
public class TopicController {
    private final TopicService topicService;
    private final BoxUserService boxUserService;

    private Long currentBoxUserId() {
        return boxUserService.getBoxIdByUserId(StpUtil.getLoginIdAsLong());
    }

    @GetMapping
    @Operation(summary = "获取我的话题列表")
    public R<List<TopicView>> list() {
        return R.ok(topicService.listOwnerTopics(currentBoxUserId()));
    }

    @PostMapping
    @Operation(summary = "创建话题")
    public R<TopicView> create(@Valid @RequestBody TopicCreateRequest request) {
        return R.ok(topicService.create(currentBoxUserId(), request));
    }

    @PostMapping("/{id}/close")
    @Operation(summary = "关闭话题")
    public R<TopicView> close(@Parameter(description = "话题 ID") @PathVariable Long id) {
        return R.ok(topicService.close(currentBoxUserId(), id));
    }
}
