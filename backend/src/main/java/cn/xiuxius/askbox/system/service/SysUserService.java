package cn.xiuxius.askbox.system.service;

import java.util.List;

import cn.xiuxius.askbox.common.PageResult;
import cn.xiuxius.askbox.security.CurrentUser;
import cn.xiuxius.askbox.system.entity.SysUserEntity;
import cn.xiuxius.askbox.system.view.UserView;

public interface SysUserService {

    /** 根据 ID 查询用户。 */
    SysUserEntity getById(Long id);

    /** 根据兼容 username 字段查询用户；邮箱身份模型下 username 等同 email。 */
    SysUserEntity getByUsername(String username);

    /** 根据邮箱查询用户。 */
    SysUserEntity getByEmail(String email);

    /** 用户认证：验证邮箱密码，返回用户。 */
    SysUserEntity authenticate(String email, String rawPassword);

    /** 管理端用户列表视图（含角色）。 */
    PageResult<UserView> listUserViews(long page, long pageSize, String keyword);

    /** 管理端用户详情视图（含角色）。 */
    UserView getUserViewById(Long id);

    /** 创建用户，返回 BCrypt 加密后的密码哈希。 */
    SysUserEntity createUser(String email, String rawPassword, String displayName, Integer topicActiveLimit);

    /** 更新用户基本信息。 */
    void updateUser(Long id, String displayName, String email, Integer topicActiveLimit);

    /** 修改当前用户密码，需验证当前密码。 */
    void changePassword(Long userId, String currentPassword, String newPassword, String confirmPassword);

    /** 禁用用户。 */
    void disableUser(Long id);

    /** 启用用户。 */
    void enableUser(Long id);

    /** 为用户分配角色（替换已有角色）。 */
    void assignRoles(Long userId, List<String> roleCodes);

    /** 加载当前用户的角色和权限信息，封装为 CurrentUser。 */
    CurrentUser loadCurrentUser(Long userId);
}
