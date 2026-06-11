package cn.xiuxius.askbox.admin.controller;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import cn.xiuxius.askbox.common.PageResult;
import cn.xiuxius.askbox.common.R;
import cn.xiuxius.askbox.security.RequiresRole;
import cn.xiuxius.askbox.system.request.AdminUserCreateRequest;
import cn.xiuxius.askbox.system.request.AdminUserUpdateRequest;
import cn.xiuxius.askbox.system.request.UserRolesUpdateRequest;
import cn.xiuxius.askbox.system.service.SysUserService;
import cn.xiuxius.askbox.system.view.UserView;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@RequiresRole("ADMIN")
@Tag(name = "系统管理接口", description = "系统管理员全局管理（需 ADMIN 角色）")
public class AdminUserController {

    private final SysUserService sysUserService;

    @GetMapping
    @Operation(summary = "用户列表", description = "分页查询所有系统用户（含角色），支持关键词搜索。")
    public R<PageResult<UserView>> listUsers(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") long page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") long pageSize,
            @Parameter(description = "搜索关键词") @RequestParam(required = false) String keyword) {
        return R.ok(sysUserService.listUserViews(page, pageSize, keyword));
    }

    @PostMapping
    @Operation(summary = "创建用户", description = "创建系统用户，返回用户信息。")
    public R<UserView> createUser(@Valid @RequestBody AdminUserCreateRequest request) {
        var user = sysUserService.createUser(request.getEmail(), request.getPassword(), request.getDisplayName());
        return R.ok(sysUserService.getUserViewById(user.getId()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "用户详情")
    public R<UserView> getUser(@Parameter(description = "用户 ID") @PathVariable Long id) {
        return R.ok(sysUserService.getUserViewById(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "编辑用户")
    public R<Void> updateUser(@PathVariable Long id, @Valid @RequestBody AdminUserUpdateRequest request) {
        sysUserService.updateUser(id, request.getDisplayName(), request.getEmail());
        return R.ok();
    }

    @PostMapping("/{id}/disable")
    @Operation(summary = "禁用用户")
    public R<Void> disableUser(@PathVariable Long id) {
        sysUserService.disableUser(id);
        return R.ok();
    }

    @PostMapping("/{id}/enable")
    @Operation(summary = "启用用户")
    public R<Void> enableUser(@PathVariable Long id) {
        sysUserService.enableUser(id);
        return R.ok();
    }

    @PutMapping("/{id}/roles")
    @Operation(summary = "分配角色", description = "为指定用户分配角色（替换已有角色列表）。")
    public R<Void> assignRoles(@PathVariable Long id, @RequestBody UserRolesUpdateRequest request) {
        sysUserService.assignRoles(id, request.getRoleCodes() == null ? java.util.List.of() : request.getRoleCodes());
        return R.ok();
    }
}
