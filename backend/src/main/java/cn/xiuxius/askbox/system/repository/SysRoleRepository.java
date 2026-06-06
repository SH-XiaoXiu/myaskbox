package cn.xiuxius.askbox.system.repository;

import java.util.List;

import org.springframework.stereotype.Repository;

import cn.xiuxius.askbox.system.entity.SysRoleEntity;
import cn.xiuxius.askbox.system.mapper.SysRoleMapper;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class SysRoleRepository {
    private final SysRoleMapper mapper;

    public List<SysRoleEntity> findAll() {
        return mapper.selectList(null);
    }

    public SysRoleEntity findByCode(String code) {
        return mapper.selectOne(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SysRoleEntity>()
                .eq(SysRoleEntity::getCode, code));
    }
}
