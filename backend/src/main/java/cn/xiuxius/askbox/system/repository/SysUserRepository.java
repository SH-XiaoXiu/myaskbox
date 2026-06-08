package cn.xiuxius.askbox.system.repository;

import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;

import cn.xiuxius.askbox.system.entity.SysUserEntity;
import cn.xiuxius.askbox.system.mapper.SysUserMapper;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class SysUserRepository {
    private final SysUserMapper mapper;

    public SysUserEntity findById(Long id) {
        return mapper.selectById(id);
    }

    public SysUserEntity findByUsername(String username) {
        return mapper.selectOne(new LambdaQueryWrapper<SysUserEntity>().eq(SysUserEntity::getUsername, username));
    }

    public SysUserEntity findByEmail(String email) {
        if (email == null || email.isBlank()) {
            return null;
        }
        return mapper.selectOne(new QueryWrapper<SysUserEntity>().apply("lower(email) = lower({0})", email));
    }

    public IPage<SysUserEntity> page(IPage<SysUserEntity> page, String keyword) {
        LambdaQueryWrapper<SysUserEntity> w = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            w.like(SysUserEntity::getUsername, keyword).or().like(SysUserEntity::getDisplayName, keyword);
        }
        w.orderByDesc(SysUserEntity::getCreatedAt);
        return mapper.selectPage(page, w);
    }

    public void insert(SysUserEntity user) {
        mapper.insert(user);
    }

    public void update(SysUserEntity user) {
        mapper.updateById(user);
    }

    public void deleteById(Long id) {
        mapper.deleteById(id);
    }

    public long countAll() {
        return mapper.selectCount(null);
    }
}
