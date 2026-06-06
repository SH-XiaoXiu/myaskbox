package cn.xiuxius.askbox.system.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 用户-角色关联 Mapper。
 * 对应 sys_user_role 表，提供 RBAC 所需的联表查询。
 */
@Mapper
public interface SysUserRoleMapper {

    /** 查询用户的角色码列表。 */
    @Select("SELECT r.code FROM sys_user_role ur JOIN sys_role r ON r.id = ur.role_id WHERE ur.user_id = #{userId}")
    List<String> selectRoleCodesByUserId(@Param("userId") Long userId);

    /** 查询用户的权限码列表。 */
    @Select("SELECT DISTINCT p.code FROM sys_user_role ur "
            + "JOIN sys_role_permission rp ON rp.role_id = ur.role_id "
            + "JOIN sys_permission p ON p.id = rp.permission_id "
            + "WHERE ur.user_id = #{userId}")
    List<String> selectPermissionCodesByUserId(@Param("userId") Long userId);

    /** 查询所有权限码（给 ADMIN 用）。 */
    @Select("SELECT code FROM sys_permission WHERE type = 'ACTION'")
    List<String> selectAllPermissionCodes();

    /** 为用户分配角色。 */
    @Insert("<script>"
            + "INSERT INTO sys_user_role (user_id, role_id) VALUES "
            + "<foreach collection='roleIds' item='roleId' separator=','>"
            + "(#{userId}, #{roleId})"
            + "</foreach>"
            + "</script>")
    void insertUserRoles(@Param("userId") Long userId, @Param("roleIds") List<Long> roleIds);

    /** 删除用户的所有角色关联。 */
    @Delete("DELETE FROM sys_user_role WHERE user_id = #{userId}")
    void deleteByUserId(@Param("userId") Long userId);

    /** 统计拥有指定角色的活跃用户数。 */
    @Select("SELECT COUNT(DISTINCT ur.user_id) FROM sys_user_role ur "
            + "JOIN sys_role r ON r.id = ur.role_id "
            + "JOIN sys_user u ON u.id = ur.user_id "
            + "WHERE r.code = #{roleCode} AND u.status = 'ACTIVE'")
    int countActiveUsersByRole(@Param("roleCode") String roleCode);

    /** 统计拥有指定角色标识的所有用户数（不计状态）。 */
    @Select("SELECT COUNT(DISTINCT ur.user_id) FROM sys_user_role ur "
            + "JOIN sys_role r ON r.id = ur.role_id "
            + "WHERE r.code = #{roleCode}")
    int countUsersByRoleCode(@Param("roleCode") String roleCode);
}
