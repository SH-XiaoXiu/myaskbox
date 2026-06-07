package cn.xiuxius.askbox.auth.controller;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cn.xiuxius.askbox.auth.request.ChangePasswordRequest;
import cn.xiuxius.askbox.auth.request.LoginRequest;
import cn.xiuxius.askbox.auth.service.AuthService;
import cn.xiuxius.askbox.auth.view.LoginView;
import cn.xiuxius.askbox.auth.view.MeView;
import cn.xiuxius.askbox.common.R;
import cn.xiuxius.askbox.common.ratelimit.RateLimit;
import cn.xiuxius.askbox.common.ratelimit.RateLimitType;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/** 统一认证接口——系统管理员和提问箱主共用同一套登录/登出。 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "认证接口", description = "统一登录、登出、当前用户信息")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @Operation(summary = "用户登录", description = "使用用户名和密码登录，系统自动识别角色。登录成功返回 Sa-Token。")
    @RateLimit(
            types = {RateLimitType.IP, RateLimitType.CUSTOM},
            key = "#request.username",
            capacity = 10,
            refillTokens = 10,
            timeWindowSeconds = 60)
    public R<LoginView> login(@Valid @RequestBody LoginRequest request) {
        return R.ok(authService.login(request.getUsername(), request.getPassword()));
    }

    @GetMapping("/me")
    @Operation(summary = "获取当前用户信息", description = "返回当前登录用户的 id、用户名、角色列表和权限列表。")
    public R<MeView> me() {
        return R.ok(authService.current());
    }

    @PostMapping("/logout")
    @Operation(summary = "退出登录", description = "注销当前用户的 Sa-Token 会话。")
    public R<Void> logout() {
        authService.logout();
        return R.ok();
    }

    @PutMapping("/password")
    @Operation(summary = "修改当前用户密码", description = "需要确认当前密码。修改成功后注销当前会话，需要重新登录。")
    public R<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(
                request.getCurrentPassword(), request.getNewPassword(), request.getConfirmPassword());
        return R.ok();
    }
}
