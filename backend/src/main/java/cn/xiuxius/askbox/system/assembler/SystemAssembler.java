package cn.xiuxius.askbox.system.assembler;

import java.util.List;

import cn.xiuxius.askbox.system.entity.SysPermissionEntity;
import cn.xiuxius.askbox.system.entity.SysRoleEntity;
import cn.xiuxius.askbox.system.entity.SysUserEntity;
import cn.xiuxius.askbox.system.view.PermissionView;
import cn.xiuxius.askbox.system.view.RoleView;
import cn.xiuxius.askbox.system.view.UserView;

public final class SystemAssembler {

    private SystemAssembler() {}

    public static UserView toUserView(SysUserEntity user, List<String> roles) {
        return new UserView(
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                user.getEmail(),
                user.getStatus(),
                user.getTopicActiveLimit() == null ? 5 : user.getTopicActiveLimit(),
                roles,
                user.getCreatedAt());
    }

    public static RoleView toRoleView(SysRoleEntity role, long userCount) {
        return new RoleView(
                role.getId(),
                role.getCode(),
                role.getName(),
                role.getDescription(),
                role.getBuiltIn(),
                userCount,
                role.getCreatedAt());
    }

    public static PermissionView toPermissionView(SysPermissionEntity permission) {
        return new PermissionView(
                permission.getId(),
                permission.getCode(),
                permission.getName(),
                permission.getParentId(),
                permission.getType(),
                permission.getSortOrder(),
                permission.getCreatedAt());
    }
}
