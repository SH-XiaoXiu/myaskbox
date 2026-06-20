package cn.xiuxius.askbox.topic.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cn.xiuxius.askbox.common.R;
import cn.xiuxius.askbox.topic.service.TopicService;
import cn.xiuxius.askbox.topic.view.PublicTopicView;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/boxes/{slug}/topics")
@RequiredArgsConstructor
@Tag(name = "公开接口", description = "公开提问箱话题")
public class PublicTopicController {
    private final TopicService topicService;

    @GetMapping
    @Operation(summary = "获取公开可参与话题")
    public R<List<PublicTopicView>> list(@Parameter(description = "提问箱 slug") @PathVariable String slug) {
        return R.ok(topicService.listPublicAvailable(slug));
    }

    @GetMapping("/{code}")
    @Operation(summary = "解析公开话题")
    public R<PublicTopicView> resolve(
            @Parameter(description = "提问箱 slug") @PathVariable String slug,
            @Parameter(description = "话题 code") @PathVariable String code) {
        return R.ok(topicService.resolvePublic(slug, code));
    }
}
