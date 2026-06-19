package cn.xiuxius.askbox.auth.service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cn.dev33.satoken.stp.SaTokenInfo;
import cn.dev33.satoken.stp.StpUtil;
import cn.xiuxius.askbox.attachment.enums.AttachmentUsageType;
import cn.xiuxius.askbox.attachment.service.AttachmentService;
import cn.xiuxius.askbox.attachment.view.AttachmentView;
import cn.xiuxius.askbox.auth.view.LoginView;
import cn.xiuxius.askbox.auth.view.MeView;
import cn.xiuxius.askbox.auth.view.RegisterConfigView;
import cn.xiuxius.askbox.boxuser.service.BoxUserService;
import cn.xiuxius.askbox.common.BizException;
import cn.xiuxius.askbox.common.ErrorCodes;
import cn.xiuxius.askbox.security.CurrentUser;
import cn.xiuxius.askbox.system.entity.SysUserEntity;
import cn.xiuxius.askbox.system.repository.SysUserRepository;
import cn.xiuxius.askbox.system.service.SysUserService;
import cn.xiuxius.askbox.system.setting.service.SysSettingService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final String CURRENT_USER_SESSION_KEY = "CURRENT_USER";

    private final SysUserService sysUserService;
    private final SysUserRepository sysUserRepository;
    private final BoxUserService boxUserService;
    private final SysSettingService sysSettingService;
    private final EmailCodeService emailCodeService;
    private final AttachmentService attachmentService;

    @Override
    @Transactional
    public LoginView login(String email, String password) {
        SysUserEntity user = sysUserService.authenticate(email, password);
        return loginUser(user);
    }

    @Override
    public void sendLoginCode(String email) {
        String normalizedEmail = normalizeEmail(email);
        SysUserEntity user = sysUserRepository.findByEmail(normalizedEmail);
        if (user == null || "DISABLED".equals(user.getStatus())) {
            return;
        }
        emailCodeService.sendLoginCode(normalizedEmail);
    }

    @Override
    @Transactional
    public LoginView loginByCode(String email, String code) {
        String normalizedEmail = normalizeEmail(email);
        SysUserEntity user = sysUserRepository.findByEmail(normalizedEmail);
        if (user == null) {
            throw new BizException(ErrorCodes.EMAIL_CODE_INVALID);
        }
        emailCodeService.verifyLoginCode(normalizedEmail, code);
        if ("DISABLED".equals(user.getStatus())) throw new BizException(ErrorCodes.FORBIDDEN, "账户已被禁用");
        return loginUser(user);
    }

    @Override
    public RegisterConfigView registerConfig() {
        return new RegisterConfigView(sysSettingService.getBoolean("registration.enabled", false));
    }

    @Override
    public void sendRegisterCode(String email) {
        ensureRegisterEnabled();
        emailCodeService.sendRegisterCode(normalizeEmail(email));
    }

    @Override
    @Transactional
    public LoginView register(String password, String confirmPassword, String email, String code, String displayName) {
        ensureRegisterEnabled();
        if (!password.equals(confirmPassword)) {
            throw new BizException(ErrorCodes.VALIDATION_ERROR, "两次输入的密码不一致");
        }
        String normalizedEmail = normalizeEmail(email);
        emailCodeService.verifyRegisterCode(normalizedEmail, code);
        SysUserEntity user = sysUserService.createUser(
                normalizedEmail,
                password,
                displayName == null || displayName.isBlank()
                        ? defaultDisplayName(normalizedEmail)
                        : displayName.trim());
        ensureOwnerBox(user);
        return loginUser(user);
    }

    private LoginView loginUser(SysUserEntity user) {
        StpUtil.login(user.getId());

        CurrentUser currentUser = sysUserService.loadCurrentUser(user.getId());
        StpUtil.getSession().set(CURRENT_USER_SESSION_KEY, currentUser);

        user.setLastLoginAt(OffsetDateTime.now(ZoneOffset.UTC));
        sysUserRepository.update(user);

        if (currentUser != null && currentUser.hasRole("BOX_OWNER")) {
            ensureOwnerBox(user);
        }

        SaTokenInfo tokenInfo = StpUtil.getTokenInfo();
        return LoginView.of(tokenInfo.getTokenValue());
    }

    @Override
    public MeView current() {
        CurrentUser current = currentUser();
        if (current == null) {
            return null;
        }
        SysUserEntity user = sysUserRepository.findById(current.id());
        return toMeView(user, current);
    }

    @Override
    @Transactional
    public MeView updateProfile(String displayName, String avatarObjectKey) {
        Long userId = StpUtil.getLoginIdAsLong();
        SysUserEntity user = requireCurrentUserEntity(userId);
        user.setDisplayName(displayName == null ? "" : displayName.trim());
        if (avatarObjectKey != null) {
            if (avatarObjectKey.isBlank()) {
                user.setAvatarAttachmentId(null);
            } else {
                AttachmentView avatar = attachmentService.createOwnedImage(
                        "account-avatar-" + userId,
                        AttachmentUsageType.ACCOUNT_AVATAR,
                        avatarObjectKey,
                        "SYS_USER",
                        userId);
                user.setAvatarAttachmentId(avatar.id());
            }
        }
        sysUserRepository.update(user);
        refreshCurrentUserSession(userId);
        return toMeView(user, currentUser());
    }

    @Override
    public void sendEmailChangeCode(String email) {
        String normalizedEmail = normalizeEmail(email);
        ensureEmailAvailable(normalizedEmail, StpUtil.getLoginIdAsLong());
        emailCodeService.sendEmailChangeCode(normalizedEmail);
    }

    @Override
    @Transactional
    public MeView changeEmail(String email, String code) {
        Long userId = StpUtil.getLoginIdAsLong();
        String normalizedEmail = normalizeEmail(email);
        ensureEmailAvailable(normalizedEmail, userId);
        emailCodeService.verifyEmailChangeCode(normalizedEmail, code);
        SysUserEntity user = requireCurrentUserEntity(userId);
        user.setUsername(normalizedEmail).setEmail(normalizedEmail);
        sysUserRepository.update(user);
        refreshCurrentUserSession(userId);
        return toMeView(user, currentUser());
    }

    @Override
    public void logout() {
        StpUtil.logout();
    }

    @Override
    @Transactional
    public void changePassword(String currentPassword, String newPassword, String confirmPassword) {
        Long userId = StpUtil.getLoginIdAsLong();
        sysUserService.changePassword(userId, currentPassword, newPassword, confirmPassword);
        StpUtil.logout();
    }

    private CurrentUser currentUser() {
        CurrentUser current = (CurrentUser) StpUtil.getSession().get(CURRENT_USER_SESSION_KEY);
        if (current != null) {
            return current;
        }
        current = sysUserService.loadCurrentUser(StpUtil.getLoginIdAsLong());
        StpUtil.getSession().set(CURRENT_USER_SESSION_KEY, current);
        return current;
    }

    private SysUserEntity requireCurrentUserEntity(Long userId) {
        SysUserEntity user = sysUserRepository.findById(userId);
        if (user == null) throw new BizException(ErrorCodes.USER_NOT_FOUND);
        return user;
    }

    private void refreshCurrentUserSession(Long userId) {
        StpUtil.getSession().delete(CURRENT_USER_SESSION_KEY);
        CurrentUser current = sysUserService.loadCurrentUser(userId);
        StpUtil.getSession().set(CURRENT_USER_SESSION_KEY, current);
    }

    private MeView toMeView(SysUserEntity user, CurrentUser current) {
        if (user == null || current == null) {
            return null;
        }
        AttachmentView avatar =
                user.getAvatarAttachmentId() == null ? null : attachmentService.getById(user.getAvatarAttachmentId());
        return new MeView(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getDisplayName(),
                avatar,
                current.roles(),
                current.permissions());
    }

    private void ensureEmailAvailable(String email, Long userId) {
        if (email.isBlank()) {
            throw new BizException(ErrorCodes.VALIDATION_ERROR, "邮箱不能为空");
        }
        SysUserEntity sameEmailUser = sysUserRepository.findByEmail(email);
        if (sameEmailUser != null && !sameEmailUser.getId().equals(userId)) {
            throw new BizException(ErrorCodes.VALIDATION_ERROR, "邮箱已存在");
        }
    }

    private void ensureOwnerBox(SysUserEntity user) {
        if (boxUserService.getByUserId(user.getId()) != null) {
            return;
        }
        String slug = defaultSlug(user);
        if (slug.isBlank()) {
            slug = "user-" + user.getId();
        }
        boxUserService.createBox(user.getId(), slug, user.getDisplayName(), "");
    }

    private void ensureRegisterEnabled() {
        if (!sysSettingService.getBoolean("registration.enabled", false)) {
            throw new BizException(ErrorCodes.REGISTER_DISABLED);
        }
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    private String defaultDisplayName(String email) {
        int at = email.indexOf('@');
        return at > 0 ? email.substring(0, at) : email;
    }

    private String defaultSlug(SysUserEntity user) {
        String email = user.getEmail() == null ? user.getUsername() : user.getEmail();
        String localPart = defaultDisplayName(normalizeEmail(email));
        return localPart.replaceAll("[^a-z0-9-]+", "-").replaceAll("(^-+|-+$)", "");
    }
}
