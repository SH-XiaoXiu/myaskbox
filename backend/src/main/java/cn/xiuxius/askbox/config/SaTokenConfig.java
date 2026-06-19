package cn.xiuxius.askbox.config;

import java.util.List;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.stp.StpUtil;
import cn.xiuxius.askbox.security.SecurityContextInterceptor;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class SaTokenConfig implements WebMvcConfigurer {

    private final SecurityContextInterceptor securityContextInterceptor;

    private static final List<String> SA_TOKEN_EXCLUDE = List.of(
            "/api/boxes/**",
            "/api/reply-tokens/**",
            "/api/attachments/anonymous-avatars",
            "/api/assets/**",
            "/api/health",
            "/api/admin/login",
            "/api/auth/login",
            "/api/auth/register",
            "/api/auth/register/**",
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/doc.html",
            "/webjars/**");

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 1. 安全上下文：登录后加载用户角色权限到 ThreadLocal
        registry.addInterceptor(securityContextInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(SA_TOKEN_EXCLUDE);

        // 2. Sa-Token 鉴权：/api/admin/** 和 /api/box/** 需登录
        registry.addInterceptor(new SaInterceptor(handle -> {
                    SaRouter.match("/api/admin/**").notMatch("/api/admin/login").check(r -> {
                        StpUtil.checkLogin();
                        StpUtil.checkRole("ADMIN");
                    });
                    SaRouter.match("/api/box/**").check(r -> {
                        StpUtil.checkLogin();
                        StpUtil.checkRole("BOX_OWNER");
                    });
                }))
                .addPathPatterns("/api/**")
                .excludePathPatterns(SA_TOKEN_EXCLUDE);
    }
}
