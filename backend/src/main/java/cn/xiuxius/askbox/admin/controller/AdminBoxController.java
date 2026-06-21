package cn.xiuxius.askbox.admin.controller;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import cn.xiuxius.askbox.box.request.BoxProfileUpdateRequest;
import cn.xiuxius.askbox.boxuser.service.BoxUserService;
import cn.xiuxius.askbox.boxuser.view.BoxProfileView;
import cn.xiuxius.askbox.boxuser.view.BoxView;
import cn.xiuxius.askbox.common.PageResult;
import cn.xiuxius.askbox.common.R;
import cn.xiuxius.askbox.security.RequiresRole;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/boxes")
@RequiredArgsConstructor
@RequiresRole("ADMIN")
@Tag(name = "系统管理接口", description = "系统管理员全局管理（需 ADMIN 角色）")
public class AdminBoxController {

    private final BoxUserService boxUserService;

    @GetMapping
    @Operation(summary = "提问箱列表")
    public R<PageResult<BoxView>> listBoxes(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(required = false) String keyword) {
        return R.ok(boxUserService.listBoxViews(page, pageSize, keyword));
    }

    @GetMapping("/{id}")
    @Operation(summary = "提问箱详情")
    public R<BoxProfileView> getBox(@PathVariable Long id) {
        return R.ok(boxUserService.getProfileById(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "编辑提问箱")
    public R<BoxProfileView> updateBox(@PathVariable Long id, @Valid @RequestBody BoxProfileUpdateRequest request) {
        return R.ok(boxUserService.updateBoxById(
                id,
                request.getSlug(),
                request.getDisplayName(),
                request.getDescription() == null ? "" : request.getDescription(),
                request.getAvatarObjectKey(),
                request.getBackgroundObjectKey(),
                request.getEmailNotifyEnabled(),
                request.getTopicActiveLimit(),
                request.getAiReviewEnabled()));
    }
}
