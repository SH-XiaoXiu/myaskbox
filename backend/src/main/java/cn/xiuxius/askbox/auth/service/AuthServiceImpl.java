package cn.xiuxius.askbox.auth.service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cn.dev33.satoken.stp.SaTokenInfo;
import cn.dev33.satoken.stp.StpUtil;
import cn.xiuxius.askbox.auth.view.LoginView;
import cn.xiuxius.askbox.auth.view.MeView;
import cn.xiuxius.askbox.boxuser.service.BoxUserService;
import cn.xiuxius.askbox.security.CurrentUser;
import cn.xiuxius.askbox.system.entity.SysUserEntity;
import cn.xiuxius.askbox.system.repository.SysUserRepository;
import cn.xiuxius.askbox.system.service.SysUserService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final String CURRENT_USER_SESSION_KEY = "CURRENT_USER";

    private final SysUserService sysUserService;
    private final SysUserRepository sysUserRepository;
    private final BoxUserService boxUserService;

    @Override
    @Transactional
    public LoginView login(String username, String password) {
        SysUserEntity user = sysUserService.authenticate(username, password);
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
        return new MeView(current.id(), current.username(), current.roles(), current.permissions());
    }

    @Override
    public void logout() {
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

    private void ensureOwnerBox(SysUserEntity user) {
        if (boxUserService.getByUserId(user.getId()) != null) {
            return;
        }
        String slug = user.getUsername().toLowerCase().replaceAll("[^a-z0-9-]", "");
        if (slug.isBlank()) {
            slug = "user-" + user.getId();
        }
        boxUserService.createBox(user.getId(), slug, user.getDisplayName(), "");
    }
}
