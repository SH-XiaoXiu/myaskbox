package cn.xiuxius.askbox.usersetting.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import cn.xiuxius.askbox.usersetting.entity.UserSettingEntity;
import cn.xiuxius.askbox.usersetting.mapper.UserSettingMapper;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class UserSettingRepository {
    private final UserSettingMapper mapper;

    public UserSettingEntity find(Long userId, String key) {
        return mapper.selectOne(new LambdaQueryWrapper<UserSettingEntity>()
                .eq(UserSettingEntity::getUserId, userId)
                .eq(UserSettingEntity::getKey, key));
    }

    public List<UserSettingEntity> findByUserIdsAndKey(Collection<Long> userIds, String key) {
        if (userIds == null || userIds.isEmpty()) {
            return List.of();
        }
        return mapper.selectList(new LambdaQueryWrapper<UserSettingEntity>()
                .in(UserSettingEntity::getUserId, userIds)
                .eq(UserSettingEntity::getKey, key));
    }

    public void upsert(UserSettingEntity setting) {
        UserSettingEntity existing = find(setting.getUserId(), setting.getKey());
        if (existing == null) {
            mapper.insert(setting);
            return;
        }
        mapper.update(
                setting,
                new LambdaQueryWrapper<UserSettingEntity>()
                        .eq(UserSettingEntity::getUserId, setting.getUserId())
                        .eq(UserSettingEntity::getKey, setting.getKey()));
    }
}
