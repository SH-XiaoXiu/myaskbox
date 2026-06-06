package cn.xiuxius.askbox.avatar.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import cn.xiuxius.askbox.avatar.service.AvatarService;
import cn.xiuxius.askbox.avatar.view.AvatarView;
import cn.xiuxius.askbox.common.R;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/** 公开接口——获取可选的匿名头像列表。前端提交问题时从中选择。 */
@RestController
@RequiredArgsConstructor
@Tag(name = "头像接口", description = "获取匿名头像列表（公开）")
public class AvatarController {

    private final AvatarService avatarService;

    @GetMapping("/api/avatars")
    @Operation(summary = "获取可选头像列表", description = "返回所有启用的预设匿名头像。前端提交问题时从中选择一个 avatarId。")
    public R<List<AvatarView>> list() {
        return R.ok(avatarService.listActive());
    }
}
