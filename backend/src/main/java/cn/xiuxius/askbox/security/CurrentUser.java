package cn.xiuxius.askbox.security;

import java.util.Set;

/**
 * 当前请求用户信息，存储在 {@link UserContextHolder} (ThreadLocal) 中。
 * <p>
 * 由 {@code AccountContextInterceptor} 在每次请求时根据 Sa-Token 会话填充，
 * 供 {@code StpInterfaceImpl} 读取权限和角色列表。
 */
public record CurrentUser(Long id, String username, String email, Set<String> roles, Set<String> permissions) {

    public boolean hasRole(String role) {
        return roles != null && roles.contains(role);
    }

    public boolean hasPermission(String permission) {
        return permissions != null && permissions.contains(permission);
    }
}
