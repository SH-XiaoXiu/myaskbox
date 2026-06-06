package cn.xiuxius.askbox.admin.controller;

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

import cn.xiuxius.askbox.avatar.request.AvatarSaveRequest;
import cn.xiuxius.askbox.avatar.service.AvatarService;
import cn.xiuxius.askbox.avatar.view.AvatarView;
import cn.xiuxius.askbox.common.PageResult;
import cn.xiuxius.askbox.common.R;
import cn.xiuxius.askbox.security.RequiresRole;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/avatars")
@RequiredArgsConstructor
@RequiresRole("ADMIN")
@Tag(name = "系统管理接口", description = "系统管理员全局管理（需 ADMIN 角色）")
public class AdminAvatarController {

    private final AvatarService avatarService;

    @GetMapping
    @Operation(summary = "头像列表（管理）")
    public R<PageResult<AvatarView>> listAvatars(
            @RequestParam(defaultValue = "1") long page, @RequestParam(defaultValue = "20") long pageSize) {
        return R.ok(avatarService.pageAll(page, pageSize));
    }

    @PostMapping
    @Operation(summary = "新增头像")
    public R<AvatarView> createAvatar(@Valid @RequestBody AvatarSaveRequest request) {
        return R.ok(avatarService.create(
                request.getName(), request.getIconBase64(), request.getBg(), request.getSortOrder()));
    }

    @PutMapping("/{id}")
    @Operation(summary = "编辑头像")
    public R<AvatarView> updateAvatar(@PathVariable Long id, @Valid @RequestBody AvatarSaveRequest request) {
        return R.ok(avatarService.update(
                id, request.getName(), request.getIconBase64(), request.getBg(), request.getSortOrder()));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除头像")
    public R<Void> deleteAvatar(@PathVariable Long id) {
        avatarService.delete(id);
        return R.ok();
    }
}
