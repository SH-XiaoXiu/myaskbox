package cn.xiuxius.askbox.system.repository;

import java.util.List;

import org.springframework.stereotype.Repository;

import cn.xiuxius.askbox.system.entity.SysPermissionEntity;
import cn.xiuxius.askbox.system.mapper.SysPermissionMapper;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class SysPermissionRepository {
    private final SysPermissionMapper mapper;

    public List<SysPermissionEntity> findAll() {
        return mapper.selectList(null);
    }
}
