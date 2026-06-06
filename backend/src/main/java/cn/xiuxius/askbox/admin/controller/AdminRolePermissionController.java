package cn.xiuxius.askbox.admin.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cn.xiuxius.askbox.common.R;
import cn.xiuxius.askbox.security.RequiresRole;
import cn.xiuxius.askbox.system.assembler.SystemAssembler;
import cn.xiuxius.askbox.system.mapper.SysUserRoleMapper;
import cn.xiuxius.askbox.system.repository.SysPermissionRepository;
import cn.xiuxius.askbox.system.repository.SysRoleRepository;
import cn.xiuxius.askbox.system.view.PermissionView;
import cn.xiuxius.askbox.system.view.RoleView;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@RequiresRole("ADMIN")
@Tag(name = "系统管理接口", description = "系统管理员全局管理（需 ADMIN 角色）")
public class AdminRolePermissionController {

    private final SysRoleRepository sysRoleRepository;
    private final SysPermissionRepository sysPermissionRepository;
    private final SysUserRoleMapper sysUserRoleMapper;

    @GetMapping("/roles")
    @Operation(summary = "角色列表")
    public R<List<RoleView>> listRoles() {
        return R.ok(sysRoleRepository.findAll().stream()
                .map(r -> SystemAssembler.toRoleView(r, sysUserRoleMapper.countUsersByRoleCode(r.getCode())))
                .toList());
    }

    @GetMapping("/permissions")
    @Operation(summary = "权限列表（扁平）")
    public R<List<PermissionView>> listPermissions() {
        return R.ok(sysPermissionRepository.findAll().stream()
                .map(SystemAssembler::toPermissionView)
                .toList());
    }
}
