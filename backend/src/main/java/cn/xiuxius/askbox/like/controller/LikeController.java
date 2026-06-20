package cn.xiuxius.askbox.like.controller;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cn.xiuxius.askbox.common.R;
import cn.xiuxius.askbox.common.ratelimit.RateLimit;
import cn.xiuxius.askbox.common.ratelimit.RateLimitType;
import cn.xiuxius.askbox.like.request.LikeBatchRequest;
import cn.xiuxius.askbox.like.request.LikeChangeRequest;
import cn.xiuxius.askbox.like.service.LikeService;
import cn.xiuxius.askbox.like.view.LikeCountView;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/likes")
@RequiredArgsConstructor
@Tag(name = "点赞接口", description = "公开问答点赞计数（无需登录）")
public class LikeController {

    private final LikeService likeService;

    @PostMapping("/change")
    @Operation(summary = "变更点赞数", description = "对公开问题或公开回复点赞/取消点赞。")
    @RateLimit(
            types = {RateLimitType.IP, RateLimitType.USER_AGENT},
            capacity = 60,
            refillTokens = 60,
            timeWindowSeconds = 60)
    public R<LikeCountView> change(@Valid @RequestBody LikeChangeRequest request) {
        return R.ok(likeService.change(request.targetType(), request.targetId(), request.delta()));
    }

    @PostMapping("/batch")
    @Operation(summary = "批量查询点赞数", description = "批量返回公开问题和公开回复的点赞数。")
    @RateLimit(
            types = {RateLimitType.IP, RateLimitType.USER_AGENT},
            capacity = 120,
            refillTokens = 120,
            timeWindowSeconds = 60)
    public R<List<LikeCountView>> batch(@Valid @RequestBody LikeBatchRequest request) {
        return R.ok(likeService.batch(request.targets()));
    }
}
