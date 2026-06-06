package cn.xiuxius.askbox.security;

import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Component;

import cn.dev33.satoken.stp.StpInterface;

/**
 * Sa-Token SPI 实现：从 {@link UserContextHolder} (ThreadLocal) 读取当前用户的角色和权限。
 * <p>
 * 实际的权限加载逻辑在 {@code SecurityContextInterceptor} 中完成（查询 DB 后存入
 * ThreadLocal），此实现只负责读取，不再查库。
 */
@Component
public class StpInterfaceImpl implements StpInterface {

    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        CurrentUser current = UserContextHolder.get();
        if (current != null && current.permissions() != null) {
            return current.permissions().stream().toList();
        }
        return Collections.emptyList();
    }

    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        CurrentUser current = UserContextHolder.get();
        if (current != null && current.roles() != null) {
            return current.roles().stream().toList();
        }
        return Collections.emptyList();
    }
}
