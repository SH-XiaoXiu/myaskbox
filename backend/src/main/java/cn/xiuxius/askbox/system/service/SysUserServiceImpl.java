package cn.xiuxius.askbox.system.service;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.StpUtil;
import cn.xiuxius.askbox.common.BizException;
import cn.xiuxius.askbox.common.ErrorCodes;
import cn.xiuxius.askbox.common.PageResult;
import cn.xiuxius.askbox.security.CurrentUser;
import cn.xiuxius.askbox.system.assembler.SystemAssembler;
import cn.xiuxius.askbox.system.entity.SysRoleEntity;
import cn.xiuxius.askbox.system.entity.SysUserEntity;
import cn.xiuxius.askbox.system.mapper.SysUserRoleMapper;
import cn.xiuxius.askbox.system.repository.SysRoleRepository;
import cn.xiuxius.askbox.system.repository.SysUserRepository;
import cn.xiuxius.askbox.system.view.UserView;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class SysUserServiceImpl implements SysUserService {

    private final SysUserRepository sysUserRepository;
    private final SysRoleRepository sysRoleRepository;
    private final SysUserRoleMapper sysUserRoleMapper;

    @Override
    public SysUserEntity getById(Long id) {
        SysUserEntity user = sysUserRepository.findById(id);
        if (user == null) throw new BizException(ErrorCodes.USER_NOT_FOUND);
        return user;
    }

    @Override
    public SysUserEntity getByUsername(String username) {
        return sysUserRepository.findByUsername(username);
    }

    @Override
    public SysUserEntity getByEmail(String email) {
        return sysUserRepository.findByEmail(normalizeEmail(email));
    }

    @Override
    public SysUserEntity authenticate(String email, String rawPassword) {
        SysUserEntity user = sysUserRepository.findByEmail(normalizeEmail(email));
        if (user == null) throw new BizException(ErrorCodes.BAD_CREDENTIALS);
        if (!BCrypt.checkpw(rawPassword, user.getPasswordHash())) throw new BizException(ErrorCodes.BAD_CREDENTIALS);
        if ("DISABLED".equals(user.getStatus())) throw new BizException(ErrorCodes.FORBIDDEN, "账户已被禁用");
        return user;
    }

    @Override
    public PageResult<UserView> listUserViews(long page, long pageSize, String keyword) {
        IPage<SysUserEntity> result = sysUserRepository.page(Page.of(page, pageSize), keyword);
        return PageResult.from(result.convert(user -> {
            List<String> roles = sysUserRoleMapper.selectRoleCodesByUserId(user.getId());
            return SystemAssembler.toUserView(user, roles);
        }));
    }

    @Override
    public UserView getUserViewById(Long id) {
        SysUserEntity user = getById(id);
        List<String> roles = sysUserRoleMapper.selectRoleCodesByUserId(id);
        return SystemAssembler.toUserView(user, roles);
    }

    @Override
    @Transactional
    public SysUserEntity createUser(String email, String rawPassword, String displayName, Integer topicActiveLimit) {
        String normalizedEmail = normalizeEmail(email);
        if (normalizedEmail.isBlank()) {
            throw new BizException(ErrorCodes.VALIDATION_ERROR, "邮箱不能为空");
        }
        if (sysUserRepository.findByEmail(normalizedEmail) != null)
            throw new BizException(ErrorCodes.VALIDATION_ERROR, "邮箱已存在");
        SysUserEntity user = new SysUserEntity()
                .setUsername(normalizedEmail)
                .setPasswordHash(BCrypt.hashpw(rawPassword, BCrypt.gensalt()))
                .setDisplayName(displayName)
                .setEmail(normalizedEmail)
                .setStatus("ACTIVE");
        sysUserRepository.insert(user);
        assignRoles(user.getId(), List.of("BOX_OWNER"));
        log.info("User created: id={} email={}", user.getId(), normalizedEmail);
        return user;
    }

    @Override
    @Transactional
    public void updateUser(Long id, String displayName, String email, Integer topicActiveLimit) {
        SysUserEntity user = getById(id);
        String normalizedEmail = normalizeEmail(email);
        if (normalizedEmail.isBlank()) {
            throw new BizException(ErrorCodes.VALIDATION_ERROR, "邮箱不能为空");
        }
        SysUserEntity sameEmailUser = sysUserRepository.findByEmail(normalizedEmail);
        if (sameEmailUser != null && !sameEmailUser.getId().equals(id)) {
            throw new BizException(ErrorCodes.VALIDATION_ERROR, "邮箱已存在");
        }
        user.setUsername(normalizedEmail).setDisplayName(displayName).setEmail(normalizedEmail);
        sysUserRepository.update(user);
    }

    @Override
    @Transactional
    public void changePassword(Long userId, String currentPassword, String newPassword, String confirmPassword) {
        SysUserEntity user = getById(userId);
        if (!BCrypt.checkpw(currentPassword, user.getPasswordHash())) {
            throw new BizException(ErrorCodes.BAD_CREDENTIALS, "当前密码不正确");
        }
        if (!newPassword.equals(confirmPassword)) {
            throw new BizException(ErrorCodes.VALIDATION_ERROR, "两次输入的新密码不一致");
        }
        if (BCrypt.checkpw(newPassword, user.getPasswordHash())) {
            throw new BizException(ErrorCodes.VALIDATION_ERROR, "新密码不能与当前密码相同");
        }
        user.setPasswordHash(BCrypt.hashpw(newPassword, BCrypt.gensalt()));
        sysUserRepository.update(user);
        log.info("User {} changed password", userId);
    }

    @Override
    @Transactional
    public void disableUser(Long id) {
        ensureNotLastAdmin(id);
        SysUserEntity user = getById(id);
        user.setStatus("DISABLED");
        sysUserRepository.update(user);
        log.info("User {} disabled", id);
    }

    @Override
    @Transactional
    public void enableUser(Long id) {
        SysUserEntity user = getById(id);
        user.setStatus("ACTIVE");
        sysUserRepository.update(user);
        log.info("User {} enabled", id);
    }

    @Override
    @Transactional
    public void assignRoles(Long userId, List<String> roleCodes) {
        // 1. 验证所有角色存在
        List<Long> roleIds = roleCodes.stream()
                .map(code -> {
                    SysRoleEntity role = sysRoleRepository.findByCode(code);
                    if (role == null) throw new BizException(ErrorCodes.VALIDATION_ERROR, "角色不存在: " + code);
                    return role.getId();
                })
                .toList();
        // 2. 删除现有角色，重新分配
        sysUserRoleMapper.deleteByUserId(userId);
        sysUserRoleMapper.insertUserRoles(userId, roleIds);
        log.info("Roles assigned to user {}: {}", userId, roleCodes);
        // 3. 清除该用户所有活跃 Session 中的权限缓存，下次请求时惰性重载
        clearUserPermissionCache(userId);
    }

    /** 清除指定用户所有活跃 Session 中的 CURRENT_USER，迫使其下次请求回退 DB 重载。 */
    private void clearUserPermissionCache(Long userId) {
        try {
            SaSession session = StpUtil.getSessionByLoginId(userId, false);
            if (session != null) {
                session.delete("CURRENT_USER");
            }
        } catch (Exception ignored) {
            // 用户可能不在线
        }
    }

    /**
     * 加载当前用户的角色和权限，封装为 CurrentUser。
     * 查询路径：sys_user → sys_user_role → sys_role → sys_role_permission → sys_permission。
     * ADMIN 角色用户自动获得全部权限。
     */
    @Override
    public CurrentUser loadCurrentUser(Long userId) {
        SysUserEntity user = sysUserRepository.findById(userId);
        if (user == null) return null;
        // 查询角色码
        List<String> roleCodes = sysUserRoleMapper.selectRoleCodesByUserId(userId);
        Set<String> roles = new HashSet<>(roleCodes);
        // ADMIN 角色 → 全部权限
        List<String> permCodes = roles.contains("ADMIN")
                ? sysUserRoleMapper.selectAllPermissionCodes()
                : sysUserRoleMapper.selectPermissionCodesByUserId(userId);
        Set<String> perms = new HashSet<>(permCodes);
        return new CurrentUser(userId, user.getUsername(), user.getEmail(), user.getDisplayName(), roles, perms);
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    /**
     * 保护性检查：不允许禁用/删除最后一个 ADMIN 用户。
     */
    private void ensureNotLastAdmin(Long userId) {
        List<String> userRoles = sysUserRoleMapper.selectRoleCodesByUserId(userId);
        if (userRoles.contains("ADMIN")) {
            int count = sysUserRoleMapper.countActiveUsersByRole("ADMIN");
            if (count <= 1) throw new BizException(ErrorCodes.FORBIDDEN, "不允许禁用最后一个系统管理员");
        }
    }
}
