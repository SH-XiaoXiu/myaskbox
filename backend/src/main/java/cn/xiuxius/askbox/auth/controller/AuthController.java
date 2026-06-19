package cn.xiuxius.askbox.auth.controller;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cn.xiuxius.askbox.auth.request.ChangePasswordRequest;
import cn.xiuxius.askbox.auth.request.EmailChangeCodeRequest;
import cn.xiuxius.askbox.auth.request.EmailChangeRequest;
import cn.xiuxius.askbox.auth.request.LoginCodeRequest;
import cn.xiuxius.askbox.auth.request.LoginCodeVerifyRequest;
import cn.xiuxius.askbox.auth.request.LoginRequest;
import cn.xiuxius.askbox.auth.request.ProfileUpdateRequest;
import cn.xiuxius.askbox.auth.request.RegisterCodeRequest;
import cn.xiuxius.askbox.auth.request.RegisterRequest;
import cn.xiuxius.askbox.auth.service.AuthService;
import cn.xiuxius.askbox.auth.view.LoginView;
import cn.xiuxius.askbox.auth.view.MeView;
import cn.xiuxius.askbox.auth.view.RegisterConfigView;
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
    @Operation(summary = "用户登录", description = "使用邮箱和密码登录，系统自动识别角色。登录成功返回 Sa-Token。")
    @RateLimit(
            types = {RateLimitType.IP, RateLimitType.CUSTOM},
            key = "#request.loginEmail",
            capacity = 10,
            refillTokens = 10,
            timeWindowSeconds = 60)
    public R<LoginView> login(@Valid @RequestBody LoginRequest request) {
        return R.ok(authService.login(request.getLoginEmail(), request.getPassword()));
    }

    @PostMapping("/login/code")
    @Operation(summary = "发送登录邮箱验证码", description = "向已有账号邮箱发送登录验证码。")
    @RateLimit(
            types = {RateLimitType.IP, RateLimitType.USER_AGENT, RateLimitType.CUSTOM},
            key = "#request.email",
            capacity = 5,
            refillTokens = 5,
            timeWindowSeconds = 60)
    public R<Void> sendLoginCode(@Valid @RequestBody LoginCodeRequest request) {
        authService.sendLoginCode(request.getEmail());
        return R.ok();
    }

    @PostMapping("/login/code/verify")
    @Operation(summary = "邮箱验证码登录", description = "校验邮箱验证码，成功后返回 Sa-Token。")
    @RateLimit(
            types = {RateLimitType.IP, RateLimitType.USER_AGENT, RateLimitType.CUSTOM},
            key = "#request.email",
            capacity = 10,
            refillTokens = 10,
            timeWindowSeconds = 60)
    public R<LoginView> loginByCode(@Valid @RequestBody LoginCodeVerifyRequest request) {
        return R.ok(authService.loginByCode(request.getEmail(), request.getCode()));
    }

    @GetMapping("/register/config")
    @Operation(summary = "获取注册配置", description = "返回当前是否开放邮箱验证码注册。")
    public R<RegisterConfigView> registerConfig() {
        return R.ok(authService.registerConfig());
    }

    @PostMapping("/register/code")
    @Operation(summary = "发送注册邮箱验证码", description = "开放注册时向邮箱发送验证码。")
    @RateLimit(
            types = {RateLimitType.IP, RateLimitType.USER_AGENT, RateLimitType.CUSTOM},
            key = "#request.email",
            capacity = 5,
            refillTokens = 5,
            timeWindowSeconds = 60)
    public R<Void> sendRegisterCode(@Valid @RequestBody RegisterCodeRequest request) {
        authService.sendRegisterCode(request.getEmail());
        return R.ok();
    }

    @PostMapping("/register")
    @Operation(summary = "邮箱验证码注册", description = "验证码通过后创建箱主账号并自动登录。")
    @RateLimit(
            types = {RateLimitType.IP, RateLimitType.USER_AGENT, RateLimitType.CUSTOM},
            key = "#request.email",
            capacity = 10,
            refillTokens = 10,
            timeWindowSeconds = 60)
    public R<LoginView> register(@Valid @RequestBody RegisterRequest request) {
        return R.ok(authService.register(
                request.getPassword(),
                request.getConfirmPassword(),
                request.getEmail(),
                request.getCode(),
                request.getDisplayName()));
    }

    @GetMapping("/me")
    @Operation(summary = "获取当前用户信息", description = "返回当前登录用户的 id、邮箱、显示名称、头像、角色列表和权限列表。")
    public R<MeView> me() {
        return R.ok(authService.current());
    }

    @PutMapping("/profile")
    @Operation(summary = "更新当前用户资料", description = "更新账号显示名称和账号头像。")
    public R<MeView> updateProfile(@Valid @RequestBody ProfileUpdateRequest request) {
        return R.ok(authService.updateProfile(request.getDisplayName(), request.getAvatarObjectKey()));
    }

    @PostMapping("/email/code")
    @Operation(summary = "发送更换邮箱验证码", description = "向新邮箱发送验证码。")
    @RateLimit(
            types = {RateLimitType.IP, RateLimitType.USER_AGENT, RateLimitType.CUSTOM},
            key = "#request.email",
            capacity = 5,
            refillTokens = 5,
            timeWindowSeconds = 60)
    public R<Void> sendEmailChangeCode(@Valid @RequestBody EmailChangeCodeRequest request) {
        authService.sendEmailChangeCode(request.getEmail());
        return R.ok();
    }

    @PutMapping("/email")
    @Operation(summary = "更换当前用户邮箱", description = "验证码通过后同时更新登录邮箱和 username。")
    public R<MeView> changeEmail(@Valid @RequestBody EmailChangeRequest request) {
        return R.ok(authService.changeEmail(request.getEmail(), request.getCode()));
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
