package cn.xiuxius.askbox.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import cn.dev33.satoken.stp.StpUtil;
import cn.xiuxius.askbox.system.service.SysUserService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 安全上下文拦截器：从 Sa-Token Session 读取缓存的用户角色权限，
 * 存入 {@link UserContextHolder} (ThreadLocal)，供 {@code StpInterfaceImpl} 读取。
 * <p>
 * 权限在登录时加载到 Session，后续请求不再查 DB。权限变更时 Session 缓存被清除，
 * 下次请求自动触发 DB 回退重载。
 * <p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SecurityContextInterceptor implements HandlerInterceptor {

    private final SysUserService sysUserService;

    @Override
    public boolean preHandle(
            @NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler) {
        try {
            Long userId = StpUtil.getLoginIdAsLong();
            // 优先从 Session 缓存读取权限（登录时已存入）
            CurrentUser currentUser = (CurrentUser) StpUtil.getSession().get("CURRENT_USER");
            // Session 中不存在 → 回退查 DB 并回填 Session（如 Redis 重启、token 跨设备等）
            if (currentUser == null) {
                currentUser = sysUserService.loadCurrentUser(userId);
                StpUtil.getSession().set("CURRENT_USER", currentUser);
            }
            UserContextHolder.set(currentUser);
        } catch (Exception e) {
            // 未登录或 token 无效，跳过设置
        }
        return true;
    }

    @Override
    public void afterCompletion(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull Object handler,
            Exception ex) {
        UserContextHolder.clear();
    }
}
