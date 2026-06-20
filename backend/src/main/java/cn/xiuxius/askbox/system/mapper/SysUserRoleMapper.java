package cn.xiuxius.askbox.system.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 用户-角色关联 Mapper。
 * 对应 sys_user_role 表，提供 RBAC 所需的联表查询。
 */
@Mapper
public interface SysUserRoleMapper {

    List<String> selectRoleCodesByUserId(@Param("userId") Long userId);

    List<String> selectPermissionCodesByUserId(@Param("userId") Long userId);

    List<String> selectAllPermissionCodes();

    void insertUserRoles(@Param("userId") Long userId, @Param("roleIds") List<Long> roleIds);

    void deleteByUserId(@Param("userId") Long userId);

    int countActiveUsersByRole(@Param("roleCode") String roleCode);

    int countUsersByRoleCode(@Param("roleCode") String roleCode);
}
