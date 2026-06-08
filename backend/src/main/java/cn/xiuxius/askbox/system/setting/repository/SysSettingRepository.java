package cn.xiuxius.askbox.system.setting.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import cn.xiuxius.askbox.system.setting.entity.SysSettingEntity;
import cn.xiuxius.askbox.system.setting.mapper.SysSettingMapper;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class SysSettingRepository {
    private final SysSettingMapper mapper;

    public List<SysSettingEntity> findAll() {
        return mapper.selectList(new LambdaQueryWrapper<SysSettingEntity>()
                .orderByAsc(SysSettingEntity::getGroupCode)
                .orderByAsc(SysSettingEntity::getKey));
    }

    public SysSettingEntity findByKey(String key) {
        return mapper.selectById(key);
    }

    public List<SysSettingEntity> findByKeys(Collection<String> keys) {
        return mapper.selectList(new LambdaQueryWrapper<SysSettingEntity>().in(SysSettingEntity::getKey, keys));
    }

    public void update(SysSettingEntity entity) {
        mapper.updateById(entity);
    }
}
